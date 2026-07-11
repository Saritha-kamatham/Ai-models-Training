// ====================================================================
// DISHES EXPLORE JS CONTROLLER
// ====================================================================

let allFoods = [];
let displayedFoods = [];
let activeCategoryFilter = 'ALL';

document.addEventListener('DOMContentLoaded', async () => {
    await fetchFoods();
});

async function fetchFoods() {
    const grid = document.getElementById('foods-grid');
    if (!grid) return;

    try {
        const response = await fetch('/foods');
        if (response.ok) {
            allFoods = await response.json();
            displayedFoods = [...allFoods];
            renderFoodsGrid();
        }
    } catch (e) {
        console.error("Fetch foods error:", e);
        grid.innerHTML = `<p style="grid-column: 1/-1; text-align: center; color: var(--danger);">Failed to load dishes.</p>`;
    }
}

function renderFoodsGrid() {
    const grid = document.getElementById('foods-grid');
    if (!grid) return;

    if (displayedFoods.length === 0) {
        grid.innerHTML = `<p style="grid-column: 1/-1; text-align: center; color: var(--text-secondary);">No dishes match your criteria.</p>`;
        return;
    }

    grid.innerHTML = displayedFoods.map(food => {
        const img = food.imageUrl || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=300&q=80';
        const buttonText = food.available ? `<i class="fas fa-shopping-cart"></i> Add` : `Out of Stock`;
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
                    <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 0.5rem;">From: <strong>${food.restaurantName}</strong></p>
                    <p>${food.description || 'No description available.'}</p>
                    <div class="card-footer">
                        <span class="price-text">₹${food.price.toFixed(2)}</span>
                        <button class="btn btn-primary" onclick="addExploreDishToCart(${food.id})" ${disabledAttr}>
                            ${buttonText}
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function handleFoodSearch(event) {
    const query = event.target.value.trim();
    performFoodSearch(query);
}

function performFoodSearch(query) {
    if (!query) {
        applyFoodFilters();
        return;
    }

    displayedFoods = allFoods.filter(f => 
        f.name.toLowerCase().includes(query.toLowerCase()) ||
        f.category.toLowerCase().includes(query.toLowerCase()) ||
        f.description.toLowerCase().includes(query.toLowerCase()) ||
        f.restaurantName.toLowerCase().includes(query.toLowerCase())
    );

    if (activeCategoryFilter !== 'ALL') {
        displayedFoods = displayedFoods.filter(f => f.category === activeCategoryFilter);
    }

    renderFoodsGrid();
}

function filterFoodByCategory(category) {
    activeCategoryFilter = category;

    const chips = document.querySelectorAll('.filter-chip');
    chips.forEach(chip => chip.classList.remove('active'));

    const safeId = category.toLowerCase().replace(/\s+/g, '-');
    const activeChip = document.getElementById(category === 'ALL' ? 'fchip-all' : `fchip-${safeId}`);
    if (activeChip) activeChip.classList.add('active');

    applyFoodFilters();
}

function applyFoodFilters() {
    const query = document.getElementById('food-search-input').value.trim();

    let result = [...allFoods];

    if (activeCategoryFilter !== 'ALL') {
        result = result.filter(f => f.category === activeCategoryFilter);
    }

    if (query) {
        result = result.filter(f => 
            f.name.toLowerCase().includes(query.toLowerCase()) ||
            f.category.toLowerCase().includes(query.toLowerCase()) ||
            f.description.toLowerCase().includes(query.toLowerCase()) ||
            f.restaurantName.toLowerCase().includes(query.toLowerCase())
        );
    }

    displayedFoods = result;
    renderFoodsGrid();
}

async function addExploreDishToCart(foodId) {
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
