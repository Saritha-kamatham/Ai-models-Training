// ====================================================================
// CUSTOMER ORDERS PAGE JS CONTROLLER
// ====================================================================

document.addEventListener('DOMContentLoaded', () => {
    if (!Session.isLoggedIn()) {
        showAlert("Please log in to view your orders!", "warning");
        window.location.href = "login.html";
        return;
    }

    if (Session.getRole() !== 'CUSTOMER') {
        showAlert("Only customers can access this page!", "warning");
        window.location.href = "index.html";
        return;
    }

    loadOrders();
});

async function loadOrders() {
    const container = document.getElementById('orders-container');
    const emptyState = document.getElementById('orders-empty-state');
    if (!container) return;

    try {
        const orders = await apiRequest('/orders');
        if (!orders || orders.length === 0) {
            container.style.display = 'none';
            emptyState.style.display = 'block';
            return;
        }

        container.style.display = 'flex';
        emptyState.style.display = 'none';

        container.innerHTML = orders.map(order => {
            const formattedDate = new Date(order.orderedAt).toLocaleString();
            
            // Map status badges
            let badgeClass = 'status-placed';
            if (order.deliveryStatus === 'PREPARING') badgeClass = 'status-preparing';
            if (order.deliveryStatus === 'OUT_FOR_DELIVERY') badgeClass = 'status-out_for_delivery';
            if (order.deliveryStatus === 'DELIVERED') badgeClass = 'status-delivered';
            if (order.deliveryStatus === 'CANCELLED') badgeClass = 'status-cancelled';

            // Items list html
            const itemsHtml = order.items.map(item => `
                <div style="display: flex; justify-content: space-between; font-size: 0.9rem; margin-bottom: 0.25rem; color: var(--text-secondary);">
                    <span>${item.foodName} (x${item.quantity})</span>
                    <span>₹${(item.price * item.quantity).toFixed(2)}</span>
                </div>
            `).join('');

            // Active timeline render if not cancelled
            let timelineHtml = '';
            if (order.deliveryStatus !== 'CANCELLED') {
                const status = order.deliveryStatus;
                
                const step1Class = getStepClass('PLACED', status);
                const step2Class = getStepClass('PREPARING', status);
                const step3Class = getStepClass('OUT_FOR_DELIVERY', status);
                const step4Class = getStepClass('DELIVERED', status);

                timelineHtml = `
                    <div class="tracking-timeline">
                        <div class="timeline-step ${step1Class}">
                            <div class="timeline-icon"><i class="fas fa-clipboard-check"></i></div>
                            <div class="timeline-label">Placed</div>
                        </div>
                        <div class="timeline-step ${step2Class}">
                            <div class="timeline-icon"><i class="fas fa-fire-burner"></i></div>
                            <div class="timeline-label">Preparing</div>
                        </div>
                        <div class="timeline-step ${step3Class}">
                            <div class="timeline-icon"><i class="fas fa-truck-ramp-box"></i></div>
                            <div class="timeline-label">On the Way</div>
                        </div>
                        <div class="timeline-step ${step4Class}">
                            <div class="timeline-icon"><i class="fas fa-house-chimney-user"></i></div>
                            <div class="timeline-label">Delivered</div>
                        </div>
                    </div>
                `;
            }

            // Cancel button logic
            const showCancelBtn = order.deliveryStatus === 'PLACED';
            const cancelBtnHtml = showCancelBtn ? `
                <button class="btn btn-danger" onclick="cancelCustomerOrder(${order.orderId})" style="padding: 0.5rem 1.2rem; font-size: 0.85rem; margin-top: 1rem;">
                    Cancel Order
                </button>
            ` : '';

            return `
                <div class="order-history-card glass-panel">
                    <div class="order-history-header">
                        <div class="order-meta">
                            <h4>Order #${order.orderId}</h4>
                            <span>Ordered At: ${formattedDate}</span>
                        </div>
                        <div style="display: flex; gap: 0.75rem; align-items: center;">
                            <span class="status-badge ${badgeClass}">${order.deliveryStatus}</span>
                            <span class="status-badge" style="background: rgba(255,255,255,0.05); color: var(--text-secondary);">Payment: ${order.paymentStatus}</span>
                        </div>
                    </div>
                    
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 2rem; border-bottom: 1px solid var(--glass-border); padding-bottom: 1.5rem;">
                        <div>
                            <h5 style="color: var(--text-muted); margin-bottom: 0.5rem; text-transform: uppercase; font-size: 0.75rem;">Items</h5>
                            ${itemsHtml}
                            <div style="display: flex; justify-content: space-between; font-weight: 700; margin-top: 0.5rem; font-size: 1rem; border-top: 1px dashed var(--glass-border); padding-top: 0.5rem;">
                                <span>Grand Total</span>
                                <span style="color: var(--primary-color);">₹${order.totalAmount.toFixed(2)}</span>
                            </div>
                        </div>
                        <div>
                            <h5 style="color: var(--text-muted); margin-bottom: 0.5rem; text-transform: uppercase; font-size: 0.75rem;">Delivery Destination</h5>
                            <p style="font-size: 0.95rem; line-height: 1.4;"><i class="fas fa-map-marker-alt" style="color: var(--primary-color); margin-right: 0.5rem;"></i> ${order.deliveryAddress}</p>
                            <p style="font-size: 0.95rem; margin-top: 0.5rem;"><i class="fas fa-store" style="color: var(--info); margin-right: 0.5rem;"></i> Resturant: <strong>${order.restaurantName}</strong></p>
                            ${cancelBtnHtml}
                        </div>
                    </div>

                    ${timelineHtml}
                </div>
            `;
        }).join('');
    } catch (e) {
        console.error("Load orders error:", e);
        container.innerHTML = `<p style="text-align: center; color: var(--danger);">Failed to load orders.</p>`;
    }
}

function getStepClass(stepName, currentStatus) {
    const statuses = ['PLACED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED'];
    const currentIdx = statuses.indexOf(currentStatus);
    const stepIdx = statuses.indexOf(stepName);

    if (currentIdx === stepIdx) {
        return 'active';
    } else if (currentIdx > stepIdx) {
        return 'completed';
    }
    return '';
}

async function cancelCustomerOrder(orderId) {
    if (!confirm("Are you sure you want to cancel this order? This action cannot be undone.")) return;

    try {
        const result = await apiRequest(`/orders/delete/${orderId}`, {
            method: 'DELETE'
        });
        showAlert("Order cancelled successfully.", "info");
        loadOrders();
    } catch (e) {
        console.error("Cancel order error:", e);
    }
}
