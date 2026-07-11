// ====================================================================
// DASHBOARD JS CONTROLLER (CRUD / ADMIN / OWNER PANEL)
// ====================================================================

let currentTab = 'overview';
let dashboardAnalytics = null;
let restaurantsList = [];
let foodsList = [];
let ordersList = [];
let customersList = [];

document.addEventListener('DOMContentLoaded', () => {
    if (!Session.isLoggedIn()) {
        showAlert("Please log in to access the dashboard!", "warning");
        window.location.href = "login.html";
        return;
    }

    const role = Session.getRole();
    if (role !== 'ADMIN' && role !== 'OWNER') {
        showAlert("Access Denied! Administrators and Restaurant Owners only.", "danger");
        window.location.href = "index.html";
        return;
    }

    // Configure sidebar links based on role
    if (role === 'ADMIN') {
        document.getElementById('tab-link-customers').style.display = 'flex';
        document.getElementById('rest-owner-selection').style.display = 'block';
    }

    loadDashboardData();
});

function switchTab(tabId) {
    currentTab = tabId;

    // Toggle active sidebar link
    const links = document.querySelectorAll('.sidebar-link');
    links.forEach(link => link.classList.remove('active'));
    document.getElementById(`tab-link-${tabId}`).classList.add('active');

    // Toggle active pane display
    const panes = document.querySelectorAll('.dashboard-pane');
    panes.forEach(pane => pane.style.display = 'none');
    document.getElementById(`pane-${tabId}`).style.display = 'block';

    // Load tab content dynamically
    if (tabId === 'overview') loadAnalytics();
    if (tabId === 'restaurants') loadRestaurants();
    if (tabId === 'foods') loadFoods();
    if (tabId === 'orders') loadOrders();
    if (tabId === 'customers') loadCustomers();
}

async function loadDashboardData() {
    await loadAnalytics();
    
    // Auto-fetch listings in background
    if (Session.getRole() === 'OWNER') {
        const ownerRests = await apiRequest('/restaurants/owner');
        restaurantsList = ownerRests || [];
    } else {
        const rests = await apiRequest('/restaurants');
        restaurantsList = rests || [];
    }
}

// 1. ANALYTICS
async function loadAnalytics() {
    try {
        const data = await apiRequest('/dashboard/analytics');
        if (data) {
            dashboardAnalytics = data;
            document.getElementById('stat-customers').innerText = data.totalCustomers;
            document.getElementById('stat-restaurants').innerText = data.totalRestaurants;
            document.getElementById('stat-foods').innerText = data.totalFoods;
            document.getElementById('stat-orders').innerText = data.totalOrders;
            document.getElementById('stat-revenue').innerText = `₹${data.totalRevenue.toFixed(2)}`;
        }
    } catch (e) {
        console.error("Load analytics failed:", e);
    }
}

// 2. RESTAURANTS
async function loadRestaurants() {
    const tbody = document.getElementById('restaurants-table-body');
    if (!tbody) return;

    try {
        const url = Session.getRole() === 'OWNER' ? '/restaurants/owner' : '/restaurants';
        const data = await apiRequest(url);
        if (data) {
            restaurantsList = data;
            tbody.innerHTML = restaurantsList.map(r => `
                <tr>
                    <td>${r.id}</td>
                    <td><strong>${r.name}</strong></td>
                    <td>${r.location}</td>
                    <td>${r.cuisine}</td>
                    <td><i class="fas fa-star" style="color: #f59e0b;"></i> ${r.rating.toFixed(1)}</td>
                    <td>${r.ownerName} (ID: ${r.ownerId})</td>
                    <td class="actions-cell">
                        <button class="action-btn action-edit" onclick="openRestaurantModal(${r.id})"><i class="fas fa-edit"></i></button>
                        ${Session.getRole() === 'ADMIN' ? `<button class="action-btn action-delete" onclick="deleteRestaurant(${r.id})"><i class="fas fa-trash-alt"></i></button>` : ''}
                    </td>
                </tr>
            `).join('');
        }
    } catch (e) {
        console.error("Load restaurants error:", e);
    }
}

function openRestaurantModal(id) {
    const modal = document.getElementById('restaurant-modal');
    const title = document.getElementById('restaurant-modal-title');
    const form = document.getElementById('restaurant-form');
    
    form.reset();
    document.getElementById('restaurant-form-id').value = '';

    if (id) {
        title.innerText = "Edit Restaurant Details";
        const r = restaurantsList.find(item => item.id === id);
        if (r) {
            document.getElementById('restaurant-form-id').value = r.id;
            document.getElementById('rest-name').value = r.name;
            document.getElementById('rest-location').value = r.location;
            document.getElementById('rest-cuisine').value = r.cuisine;
            document.getElementById('rest-image').value = r.imageUrl || '';
            if (Session.getRole() === 'ADMIN') {
                document.getElementById('rest-owner-id').value = r.ownerId;
            }
        }
    } else {
        title.innerText = "Add New Restaurant";
    }

    modal.classList.add('active');
}

function closeRestaurantModal() {
    document.getElementById('restaurant-modal').classList.remove('active');
}

async function saveRestaurantForm(event) {
    event.preventDefault();

    const id = document.getElementById('restaurant-form-id').value;
    const name = document.getElementById('rest-name').value.trim();
    const location = document.getElementById('rest-location').value.trim();
    const cuisine = document.getElementById('rest-cuisine').value.trim();
    const imageUrl = document.getElementById('rest-image').value.trim();
    
    const payload = { name, location, cuisine, imageUrl };
    
    if (Session.getRole() === 'ADMIN') {
        const ownerId = document.getElementById('rest-owner-id').value;
        if (ownerId) payload.ownerId = parseInt(ownerId);
    }

    try {
        let result;
        if (id) {
            result = await apiRequest(`/restaurants/update/${id}`, {
                method: 'PUT',
                body: JSON.stringify(payload)
            });
            showAlert("Restaurant updated successfully!", "success");
        } else {
            result = await apiRequest('/restaurants/add', {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            showAlert("Restaurant added successfully!", "success");
        }

        if (result) {
            closeRestaurantModal();
            loadRestaurants();
            loadAnalytics();
        }
    } catch (e) {
        console.error("Save restaurant error:", e);
    }
}

async function deleteRestaurant(id) {
    if (!confirm("Are you sure you want to delete this restaurant? All menu items and dependencies will be removed.")) return;

    try {
        await apiRequest(`/restaurants/delete/${id}`, { method: 'DELETE' });
        showAlert("Restaurant deleted successfully.", "info");
        loadRestaurants();
        loadAnalytics();
    } catch (e) {
        console.error("Delete restaurant error:", e);
    }
}


// 3. FOOD ITEMS
async function loadFoods() {
    const tbody = document.getElementById('foods-table-body');
    if (!tbody) return;

    try {
        const data = await apiRequest('/foods');
        if (data) {
            // Filter foods based on owner's restaurants if logged in as OWNER
            if (Session.getRole() === 'OWNER') {
                const ownerRestIds = restaurantsList.map(r => r.id);
                foodsList = data.filter(food => ownerRestIds.includes(food.restaurantId));
            } else {
                foodsList = data;
            }

            tbody.innerHTML = foodsList.map(f => `
                <tr>
                    <td>${f.id}</td>
                    <td><strong>${f.name}</strong></td>
                    <td>${f.restaurantName} (ID: ${f.restaurantId})</td>
                    <td>${f.category}</td>
                    <td>₹${f.price.toFixed(2)}</td>
                    <td>
                        <span class="status-badge" style="background: ${f.available ? 'rgba(16, 185, 129, 0.15); color: var(--success);' : 'rgba(239, 68, 68, 0.15); color: var(--danger);'}">
                            ${f.available ? 'Available' : 'Unavailable'}
                        </span>
                    </td>
                    <td class="actions-cell">
                        <button class="action-btn action-edit" onclick="openFoodModal(${f.id})"><i class="fas fa-edit"></i></button>
                        <button class="action-btn action-delete" onclick="deleteFood(${f.id})"><i class="fas fa-trash-alt"></i></button>
                    </td>
                </tr>
            `).join('');
        }
    } catch (e) {
        console.error("Load foods error:", e);
    }
}

function openFoodModal(id) {
    const modal = document.getElementById('food-modal');
    const title = document.getElementById('food-modal-title');
    const form = document.getElementById('food-form');
    const restSelect = document.getElementById('food-rest-id');

    form.reset();
    document.getElementById('food-form-id').value = '';

    // Populate restaurant drop-down
    if (restaurantsList.length === 0) {
        showAlert("Please register a restaurant first before adding food items!", "warning");
        return;
    }
    
    restSelect.innerHTML = restaurantsList.map(r => `
        <option value="${r.id}">${r.name} (ID: ${r.id})</option>
    `).join('');

    if (id) {
        title.innerText = "Edit Menu Item";
        const f = foodsList.find(item => item.id === id);
        if (f) {
            document.getElementById('food-form-id').value = f.id;
            restSelect.value = f.restaurantId;
            document.getElementById('food-name').value = f.name;
            document.getElementById('food-category').value = f.category;
            document.getElementById('food-price').value = f.price;
            document.getElementById('food-desc').value = f.description || '';
            document.getElementById('food-image').value = f.imageUrl || '';
            document.getElementById('food-available').checked = f.available;
        }
    } else {
        title.innerText = "Add New Menu Item";
    }

    modal.classList.add('active');
}

function closeFoodModal() {
    document.getElementById('food-modal').classList.remove('active');
}

async function saveFoodForm(event) {
    event.preventDefault();

    const id = document.getElementById('food-form-id').value;
    const restaurantId = parseInt(document.getElementById('food-rest-id').value);
    const name = document.getElementById('food-name').value.trim();
    const category = document.getElementById('food-category').value.trim();
    const price = parseFloat(document.getElementById('food-price').value);
    const description = document.getElementById('food-desc').value.trim();
    const imageUrl = document.getElementById('food-image').value.trim();
    const available = document.getElementById('food-available').checked;

    const payload = { restaurantId, name, category, price, description, imageUrl, available };

    try {
        let result;
        if (id) {
            result = await apiRequest(`/foods/update/${id}`, {
                method: 'PUT',
                body: JSON.stringify(payload)
            });
            showAlert("Food item updated successfully!", "success");
        } else {
            result = await apiRequest('/foods/add', {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            showAlert("Food item added successfully!", "success");
        }

        if (result) {
            closeFoodModal();
            loadFoods();
            loadAnalytics();
        }
    } catch (e) {
        console.error("Save food error:", e);
    }
}

async function deleteFood(id) {
    if (!confirm("Are you sure you want to delete this food item from the menu?")) return;

    try {
        await apiRequest(`/foods/delete/${id}`, { method: 'DELETE' });
        showAlert("Menu item deleted successfully.", "info");
        loadFoods();
        loadAnalytics();
    } catch (e) {
        console.error("Delete food error:", e);
    }
}


// 4. ORDERS
async function loadOrders() {
    const tbody = document.getElementById('orders-table-body');
    if (!tbody) return;

    try {
        const data = await apiRequest('/orders');
        if (data) {
            ordersList = data;
            tbody.innerHTML = ordersList.map(o => {
                const date = new Date(o.orderedAt).toLocaleString();
                
                let badgeClass = 'status-placed';
                if (o.deliveryStatus === 'PREPARING') badgeClass = 'status-preparing';
                if (o.deliveryStatus === 'OUT_FOR_DELIVERY') badgeClass = 'status-out_for_delivery';
                if (o.deliveryStatus === 'DELIVERED') badgeClass = 'status-delivered';
                if (o.deliveryStatus === 'CANCELLED') badgeClass = 'status-cancelled';

                return `
                    <tr>
                        <td><strong>#${o.orderId}</strong></td>
                        <td>${o.customerName} (ID: ${o.customerId})</td>
                        <td>${o.restaurantName} (ID: ${o.restaurantId})</td>
                        <td><strong style="color: var(--primary-color);">₹${o.totalAmount.toFixed(2)}</strong></td>
                        <td>${date}</td>
                        <td>
                            <span class="status-badge" style="background: rgba(255,255,255,0.05); color: var(--text-secondary); font-size: 0.75rem;">
                                ${o.paymentStatus}
                            </span>
                        </td>
                        <td><span class="status-badge ${badgeClass}">${o.deliveryStatus}</span></td>
                        <td class="actions-cell">
                            <button class="action-btn action-edit" onclick="openOrderModal(${o.orderId})"><i class="fas fa-truck"></i> Status</button>
                        </td>
                    </tr>
                `;
            }).join('');
        }
    } catch (e) {
        console.error("Load orders error:", e);
    }
}

function openOrderModal(id) {
    const modal = document.getElementById('order-modal');
    document.getElementById('order-form-id').value = id;

    const o = ordersList.find(item => item.orderId === id);
    if (o) {
        document.getElementById('order-delivery-status').value = o.deliveryStatus;
        document.getElementById('order-payment-status').value = o.paymentStatus;
    }

    modal.classList.add('active');
}

function closeOrderModal() {
    document.getElementById('order-modal').classList.remove('active');
}

async function saveOrderStatusForm(event) {
    event.preventDefault();

    const id = document.getElementById('order-form-id').value;
    const deliveryStatus = document.getElementById('order-delivery-status').value;
    const paymentStatus = document.getElementById('order-payment-status').value;

    try {
        const result = await apiRequest(`/orders/update/${id}`, {
            method: 'PUT',
            body: JSON.stringify({ deliveryStatus, paymentStatus })
        });

        if (result) {
            showAlert("Order status updated successfully!", "success");
            closeOrderModal();
            loadOrders();
            loadAnalytics();
        }
    } catch (e) {
        console.error("Update order status error:", e);
    }
}


// 5. CUSTOMERS (ADMIN ONLY)
async function loadCustomers() {
    const tbody = document.getElementById('customers-table-body');
    if (!tbody) return;

    try {
        const data = await apiRequest('/customers');
        if (data) {
            customersList = data;
            tbody.innerHTML = customersList.map(c => `
                <tr>
                    <td>${c.id}</td>
                    <td><strong>${c.fullName}</strong></td>
                    <td>${c.email}</td>
                    <td>${c.phone}</td>
                    <td>${c.city || 'N/A'}</td>
                    <td class="actions-cell">
                        <button class="action-btn action-delete" onclick="deleteCustomer(${c.id})"><i class="fas fa-trash-alt"></i></button>
                    </td>
                </tr>
            `).join('');
        }
    } catch (e) {
        console.error("Load customers error:", e);
    }
}

async function deleteCustomer(id) {
    if (!confirm("Are you sure you want to delete this customer account? All active carts and orders will be removed.")) return;

    try {
        await apiRequest(`/customers/delete/${id}`, { method: 'DELETE' });
        showAlert("Customer account deleted successfully.", "info");
        loadCustomers();
        loadAnalytics();
    } catch (e) {
        console.error("Delete customer error:", e);
    }
}
