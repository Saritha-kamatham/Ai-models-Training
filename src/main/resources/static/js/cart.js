// ====================================================================
// CART PAGE JS CONTROLLER
// ====================================================================

document.addEventListener('DOMContentLoaded', () => {
    if (!Session.isLoggedIn()) {
        showAlert("Please log in to view your cart!", "warning");
        window.location.href = "login.html";
        return;
    }

    if (Session.getRole() !== 'CUSTOMER') {
        showAlert("Only customers can access the shopping cart!", "warning");
        window.location.href = "index.html";
        return;
    }

    loadCart();
});

async function loadCart() {
    try {
        const cart = await apiRequest('/cart');
        if (!cart) return;

        renderCart(cart);
    } catch (e) {
        console.error("Cart loading error:", e);
    }
}

function renderCart(cart) {
    const wrapper = document.getElementById('cart-content-wrapper');
    const emptyState = document.getElementById('cart-empty-state');
    const itemsContainer = document.getElementById('cart-items-container');
    const summaryContainer = document.getElementById('cart-summary-container');

    if (!cart.items || cart.items.length === 0) {
        if (wrapper) wrapper.style.display = 'none';
        if (emptyState) emptyState.style.display = 'block';
        return;
    }

    if (wrapper) wrapper.style.display = 'grid';
    if (emptyState) emptyState.style.display = 'none';

    // Render items list
    itemsContainer.innerHTML = cart.items.map(item => {
        const img = item.imageUrl || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=300&q=80';
        return `
            <div class="cart-item glass-panel">
                <img src="${img}" alt="${item.foodName}" class="cart-item-img">
                <div class="cart-item-details">
                    <h4>${item.foodName}</h4>
                    <p style="color: var(--text-primary); font-weight: 600; margin-top: 0.25rem;">₹${item.price.toFixed(2)}</p>
                </div>
                
                <div class="quantity-control">
                    <button class="qty-btn" onclick="updateItemQuantity(${item.id}, ${item.quantity - 1})">-</button>
                    <span style="font-weight: 600; font-size: 1rem; min-width: 20px; text-align: center;">${item.quantity}</span>
                    <button class="qty-btn" onclick="updateItemQuantity(${item.id}, ${item.quantity + 1})">+</button>
                </div>
                
                <span style="font-weight: 700; font-size: 1.1rem; min-width: 80px; text-align: right; color: var(--primary-color);">₹${item.totalPrice.toFixed(2)}</span>
                
                <button class="remove-item-btn" onclick="removeCartItem(${item.id})">
                    <i class="fas fa-trash-alt"></i>
                </button>
            </div>
        `;
    }).join('');

    // Render summary details
    const subtotal = cart.grandTotal;
    const deliveryFee = subtotal > 300 ? 0.0 : 40.0;
    const tax = subtotal * 0.08; // 8% tax
    const grandTotal = subtotal + deliveryFee + tax;

    summaryContainer.innerHTML = `
        <h3 style="margin-bottom: 1.5rem; font-size: 1.4rem;">Bill Details</h3>
        <div class="summary-row">
            <span>Item Subtotal</span>
            <span>₹${subtotal.toFixed(2)}</span>
        </div>
        <div class="summary-row">
            <span>Delivery Partner Fee</span>
            <span>${deliveryFee === 0 ? '<span style="color: var(--success); font-weight: 600;">FREE</span>' : `₹${deliveryFee.toFixed(2)}`}</span>
        </div>
        <div class="summary-row">
            <span>Taxes & Charges (8%)</span>
            <span>₹${tax.toFixed(2)}</span>
        </div>
        
        <div class="summary-row summary-total">
            <span>Grand Total</span>
            <span style="color: var(--primary-color);">₹${grandTotal.toFixed(2)}</span>
        </div>
        
        <button class="btn btn-primary" onclick="proceedToCheckout()" style="width: 100%; margin-top: 1.5rem; padding: 0.9rem;">
            Proceed to Checkout <i class="fas fa-arrow-right"></i>
        </button>
    `;
}

async function updateItemQuantity(cartItemId, newQty) {
    try {
        const cart = await apiRequest(`/cart/update/${cartItemId}?quantity=${newQty}`, {
            method: 'PUT'
        });
        if (cart) {
            renderCart(cart);
            updateCartBadgeCount();
        }
    } catch (e) {
        console.error("Cart update quantity error:", e);
    }
}

async function removeCartItem(cartItemId) {
    if (!confirm("Are you sure you want to remove this item from your cart?")) return;
    try {
        const cart = await apiRequest(`/cart/delete/${cartItemId}`, {
            method: 'DELETE'
        });
        if (cart) {
            renderCart(cart);
            updateCartBadgeCount();
            showAlert("Item removed from cart.", "info");
        }
    } catch (e) {
        console.error("Cart remove error:", e);
    }
}

function proceedToCheckout() {
    window.location.href = "checkout.html";
}
