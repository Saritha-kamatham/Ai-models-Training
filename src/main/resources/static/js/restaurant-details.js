// ====================================================================
// RESTAURANT DETAILS & MENU JS CONTROLLER
// ====================================================================

let restaurantId = null;
let restaurantInfo = null;
let menuItems = [];
let displayedMenuItems = [];

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    restaurantId = urlParams.get('id');
    
    if (restaurantId) {
        loadRestaurantDetails();
    } else {
        window.location.href = 'restaurants.html';
    }
});

async function loadRestaurantDetails() {
    try {
        // Fetch restaurant details
        const response = await fetch(`/restaurants/${restaurantId}`);
        if (!response.ok) {
            throw new Error("Failed to load restaurant details.");
        }
        restaurantInfo = await response.json();
        renderBanner();

        // Fetch menu items
        const menuResponse = await fetch(`/foods/restaurant/${restaurantId}`);
        if (menuResponse.ok) {
            menuItems = await menuResponse.json();
            displayedMenuItems = [...menuItems];
            
            buildCategoriesSidebar();
            renderMenuItems();
        }
    } catch (e) {
        console.error("Restaurant load error:", e);
        showAlert("Failed to load restaurant menu details.", "danger");
    }
}

function renderBanner() {
    const banner = document.getElementById('restaurant-banner-container');
    if (!banner) return;

    const img = restaurantInfo.imageUrl || 'https://images.unsplash.com/photo-1552566626-52f8b828add9?auto=format&fit=crop&w=500&q=80';
    
    banner.innerHTML = `
        <img src="${img}" alt="${restaurantInfo.name}">
        <div class="banner-info">
            <h2>${restaurantInfo.name}</h2>
            <p style="color: var(--text-secondary);"><i class="fas fa-utensils"></i> ${restaurantInfo.cuisine}</p>
            <div class="banner-meta">
                <span><i class="fas fa-map-marker-alt"></i> ${restaurantInfo.location}</span>
                <span><i class="fas fa-star" style="color: #f59e0b;"></i> ${restaurantInfo.rating > 0 ? restaurantInfo.rating.toFixed(1) : 'New'}</span>
            </div>
        </div>
    `;
}

function buildCategoriesSidebar() {
    const sidebar = document.getElementById('menu-categories-sidebar');
    if (!sidebar) return;

    // Get unique categories from menu items
    const categories = [...new Set(menuItems.map(item => item.category))];
    
    // Reset sidebar to initial 'All Items'
    sidebar.innerHTML = `<button class="category-btn active" onclick="filterMenu('ALL')" id="cat-all">All Items</button>`;
    
    sidebar.innerHTML += categories.map(cat => {
        const safeId = cat.toLowerCase().replace(/\s+/g, '-');
        return `
            <button class="category-btn" onclick="filterMenu('${cat}')" id="cat-${safeId}">${cat}</button>
        `;
    }).join('');
}

function filterMenu(category) {
    const title = document.getElementById('current-category-title');
    if (title) title.innerText = category === 'ALL' ? 'All Items' : category;

    // Manage active sidebar buttons
    const btns = document.querySelectorAll('.category-btn');
    btns.forEach(btn => btn.classList.remove('active'));

    const safeId = category.toLowerCase().replace(/\s+/g, '-');
    const activeBtn = document.getElementById(category === 'ALL' ? 'cat-all' : `cat-${safeId}`);
    if (activeBtn) activeBtn.classList.add('active');

    // Filter list
    if (category === 'ALL') {
        displayedMenuItems = [...menuItems];
    } else {
        displayedMenuItems = menuItems.filter(item => item.category === category);
    }
    renderMenuItems();
}

function renderMenuItems() {
    const grid = document.getElementById('menu-items-grid');
    if (!grid) return;

    if (displayedMenuItems.length === 0) {
        grid.innerHTML = `<p style="text-align: center; color: var(--text-secondary); grid-column: 1/-1;">No food items available in this category.</p>`;
        return;
    }

    grid.innerHTML = displayedMenuItems.map(food => {
        const img = food.imageUrl || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=300&q=80';
        const buttonText = food.available ? `<i class="fas fa-shopping-cart"></i> Add to Cart` : `Out of Stock`;
        const disabledAttr = food.available ? '' : 'disabled style="opacity: 0.5; background: #64748b; cursor: not-allowed;"';
        
        return `
            <div class="card glass-panel">
                <div class="card-img-wrapper">
                    <img src="${img}" alt="${food.name}" style="${food.available ? '' : 'filter: grayscale(1);'}">
                    <div class="card-badge-top" style="background: rgba(15, 23, 42, 0.75); backdrop-filter: blur(4px);">
                        ${food.category}
                    </div>
                </div>
                <div class="card-body">
                    <h3>${food.name}</h3>
                    <p>${food.description || 'No description available.'}</p>
                    <div class="card-footer">
                        <span class="price-text">₹${food.price.toFixed(2)}</span>
                        <button class="btn btn-primary" onclick="addMenuDishToCart(${food.id})" ${disabledAttr}>
                            ${buttonText}
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

async function addMenuDishToCart(foodId) {
    if (!Session.isLoggedIn()) {
        showAlert("Please log in to add items to your cart!", "warning");
        setTimeout(() => {
            window.location.href = "login.html";
        }, 1500);
        return;
    }

    if (Session.getRole() !== 'CUSTOMER') {
        showAlert("Only customers can purchase food items!", "warning");
        return;
    }

    try {
        const result = await apiRequest('/cart/add', {
            method: 'POST',
            body: JSON.stringify({ foodId: foodId, quantity: 1 })
        });
        if (result) {
            showAlert("Item added to cart successfully!", "success");
            updateCartBadgeCount();
        }
    } catch (e) {
        console.error("Cart add error:", e);
    }
}
