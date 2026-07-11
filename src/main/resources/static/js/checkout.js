// ====================================================================
// CHECKOUT PAGE JS CONTROLLER
// ====================================================================

let cartData = null;

document.addEventListener('DOMContentLoaded', async () => {
    if (!Session.isLoggedIn()) {
        showAlert("Please log in to checkout!", "warning");
        window.location.href = "login.html";
        return;
    }

    if (Session.getRole() !== 'CUSTOMER') {
        showAlert("Only customers can place orders!", "warning");
        window.location.href = "index.html";
        return;
    }

    autofillUserDetails();
    await loadCheckoutSummary();
});

async function autofillUserDetails() {
    try {
        const userId = Session.getUserId();
        // Fetch fresh details from profile API
        const user = await apiRequest(`/customers/${userId}`);
        if (user) {
            document.getElementById('checkout-address').value = user.address || '';
            document.getElementById('checkout-city').value = user.city || '';
        }
    } catch (e) {
        console.error("Autofill profile details failed:", e);
    }
}

async function loadCheckoutSummary() {
    const container = document.getElementById('checkout-summary-container');
    if (!container) return;

    try {
        cartData = await apiRequest('/cart');
        if (!cartData || !cartData.items || cartData.items.length === 0) {
            showAlert("Your cart is empty! Add items first.", "warning");
            window.location.href = "restaurants.html";
            return;
        }

        const subtotal = cartData.grandTotal;
        const deliveryFee = subtotal > 300 ? 0.0 : 40.0;
        const tax = subtotal * 0.08;
        const grandTotal = subtotal + deliveryFee + tax;

        // Render summary list and total
        let itemsHtml = cartData.items.map(item => `
            <div style="display: flex; justify-content: space-between; font-size: 0.9rem; margin-bottom: 0.5rem; color: var(--text-secondary);">
                <span>${item.foodName} (x${item.quantity})</span>
                <span>₹${item.totalPrice.toFixed(2)}</span>
            </div>
        `).join('');

        container.innerHTML = `
            <h3 style="margin-bottom: 1.5rem; font-size: 1.4rem;">Order Review</h3>
            
            <div style="border-bottom: 1px solid var(--glass-border); padding-bottom: 1rem; margin-bottom: 1rem;">
                ${itemsHtml}
            </div>

            <div class="summary-row">
                <span>Subtotal</span>
                <span>₹${subtotal.toFixed(2)}</span>
            </div>
            <div class="summary-row">
                <span>Delivery Partner Fee</span>
                <span>${deliveryFee === 0 ? '<span style="color: var(--success); font-weight: 600;">FREE</span>' : `₹${deliveryFee.toFixed(2)}`}</span>
            </div>
            <div class="summary-row">
                <span>GST & Service Taxes</span>
                <span>₹${tax.toFixed(2)}</span>
            </div>
            
            <div class="summary-row summary-total">
                <span>Grand Total</span>
                <span style="color: var(--primary-color);">₹${grandTotal.toFixed(2)}</span>
            </div>
            
            <button class="btn btn-primary" onclick="placeOrder()" style="width: 100%; margin-top: 1.5rem; padding: 0.95rem;">
                Place Order & Pay
            </button>
        `;
    } catch (e) {
        console.error("Checkout summary load error:", e);
    }
}

async function placeOrder() {
    const address = document.getElementById('checkout-address').value.trim();
    const city = document.getElementById('checkout-city').value.trim();
    
    if (!address || !city) {
        showAlert("Please fill in your delivery address and city!", "warning");
        return;
    }

    // Get selected payment method
    const paymentMethodEl = document.querySelector('input[name="payment-method"]:checked');
    const paymentMethod = paymentMethodEl ? paymentMethodEl.value : 'COD';

    try {
        const fullAddress = `${address}, ${city}`;
        const payload = {
            deliveryAddress: fullAddress,
            paymentMethod: paymentMethod
        };

        const result = await apiRequest('/orders/add', {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        if (result) {
            showAlert("Order placed successfully!", "success");
            
            // Show custom delivery tracking transition overlay
            showLoader();
            const loaderPara = document.querySelector('#global-loader p');
            if (loaderPara) loaderPara.innerText = "Simulating Secure Payment Verification...";

            setTimeout(() => {
                hideLoader();
                window.location.href = "orders.html";
            }, 2000);
        }
    } catch (e) {
        console.error("Order placement failed:", e);
    }
}
