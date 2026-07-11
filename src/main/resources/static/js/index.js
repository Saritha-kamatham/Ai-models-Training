// ====================================================================
// HOME PAGE JS CONTROLLER
// ====================================================================

document.addEventListener('DOMContentLoaded', () => {
    loadPopularRestaurants();
    loadTrendingDishes();
    
    // Listen to global location changes
    window.addEventListener('locationChanged', () => {
        loadPopularRestaurants();
        loadTrendingDishes();
    });
    
    // Add enter-key listener for search bar
    const searchInput = document.getElementById('hero-search-input');
    if (searchInput) {
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                handleHeroSearch();
            }
        });
    }
});

async function loadPopularRestaurants() {
    const container = document.getElementById('popular-restaurants-container');
    if (!container) return;

    try {
        const response = await fetch('/restaurants');
        if (response.ok) {
            const restaurants = await response.json();
            
            const userLat = parseFloat(localStorage.getItem('selected_lat'));
            const userLng = parseFloat(localStorage.getItem('selected_lng'));
            
            let filtered = restaurants.map(r => {
                if (userLat && userLng && r.latitude && r.longitude) {
                    r.distance = calculateDistance(userLat, userLng, r.latitude, r.longitude);
                } else {
                    r.distance = null;
                }
                return r;
            });

            // Filter: If user set location, show only within 15 km
            if (userLat && userLng) {
                let nearby = filtered.filter(r => r.distance !== null && r.distance <= 15.0);
                if (nearby.length === 0) {
                    const address = localStorage.getItem('selected_address') || '';
                    const townName = address.split(',')[0].trim() || 'Local';
                    filtered = filtered.map((r, index) => {
                        if (!r.originalName) r.originalName = r.name;
                        if (!r.originalLocation) r.originalLocation = r.location;

                        const angle = (index * 2 * Math.PI) / filtered.length;
                        const offsetLat = 0.006 * Math.sin(angle);
                        const offsetLng = 0.006 * Math.cos(angle);
                        r.latitude = userLat + offsetLat;
                        r.longitude = userLng + offsetLng;
                        r.distance = calculateDistance(userLat, userLng, r.latitude, r.longitude);
                        
                        // Localize location
                        r.location = `${r.originalLocation.split(',')[0]}, ${townName}`;

                        // Localize name (e.g. Cafe Bombay -> Cafe Pune)
                        let localizedName = r.originalName;
                        if (localizedName.includes("Bombay")) {
                            localizedName = localizedName.replace("Bombay", townName);
                        } else if (localizedName.includes("Bangalore")) {
                            localizedName = localizedName.replace("Bangalore", townName);
                        } else if (localizedName.includes("Madanapalle")) {
                            localizedName = localizedName.replace("Madanapalle", townName);
                        } else if (localizedName.includes("Anantapur")) {
                            localizedName = localizedName.replace("Anantapur", townName);
                        } else if (localizedName.includes("Pune")) {
                            localizedName = localizedName.replace("Pune", townName);
                        } else if (localizedName.includes("Zone") || localizedName.includes("Palace") || localizedName.includes("Point") || localizedName.includes("Town") || localizedName.includes("Grand") || localizedName.includes("Express")) {
                            if (!localizedName.includes(townName)) {
                                localizedName = `${townName} ${localizedName}`;
                            }
                        } else {
                            if (!localizedName.includes(townName)) {
                                localizedName = `${localizedName} (${townName})`;
                            }
                        }
                        r.name = localizedName;
                        return r;
                    });
                } else {
                    filtered = nearby.map(r => {
                        if (r.originalName) r.name = r.originalName;
                        if (r.originalLocation) r.location = r.originalLocation;
                        return r;
                    });
                }
            }

            // Sort by rating (high to low) and take top 4
            const popular = filtered
                .sort((a, b) => b.rating - a.rating)
                .slice(0, 4);

            if (popular.length === 0) {
                const hasLocation = localStorage.getItem('selected_lat') !== null;
                const msg = hasLocation
                    ? "No restaurants found nearby (within 15 km). Click 'Set Location' in the header to change your location!"
                    : "No restaurants registered yet.";
                container.innerHTML = `<p style="grid-column: 1/-1; text-align: center; color: var(--text-secondary); max-width: 500px; margin: 1.5rem auto; line-height: 1.5;">${msg}</p>`;
                return;
            }

            container.innerHTML = popular.map(restaurant => {
                const img = restaurant.imageUrl || 'https://images.unsplash.com/photo-1552566626-52f8b828add9?auto=format&fit=crop&w=500&q=80';
                let distanceHtml = '';
                if (restaurant.distance !== undefined && restaurant.distance !== null) {
                    distanceHtml = `
                        <div class="distance-badge">
                            <i class="fas fa-motorcycle"></i> ${restaurant.distance.toFixed(1)} km away
                        </div>
                    `;
                }
                
                return `
                    <div class="card glass-panel">
                        <div class="card-img-wrapper">
                            <img src="${img}" alt="${restaurant.name}">
                            <div class="rating-badge">
                                <i class="fas fa-star"></i> ${restaurant.rating > 0 ? restaurant.rating.toFixed(1) : 'New'}
                            </div>
                        </div>
                        <div class="card-body">
                            <h3>${restaurant.name}</h3>
                            <p style="color: var(--text-secondary); margin-bottom: 0.25rem;"><i class="fas fa-utensils"></i> ${restaurant.cuisine}</p>
                            <p style="color: var(--text-muted); font-size: 0.85rem;"><i class="fas fa-map-marker-alt"></i> ${restaurant.location}</p>
                            ${distanceHtml}
                            <div class="card-footer" style="border: none; padding-top: 0;">
                                <a href="restaurant-details.html?id=${restaurant.id}" class="btn btn-primary" style="width: 100%; margin-top: 1rem;">View Menu</a>
                            </div>
                        </div>
                    </div>
                `;
            }).join('');
        }
    } catch (e) {
        console.error("Failed to load popular restaurants:", e);
        container.innerHTML = `<p style="grid-column: 1/-1; text-align: center; color: var(--danger);">Failed to load restaurants.</p>`;
    }
}

async function loadTrendingDishes() {
    const container = document.getElementById('trending-dishes-container');
    if (!container) return;

    try {
        const response = await fetch('/foods');
        if (response.ok) {
            const foods = await response.json();
            
            const userLat = parseFloat(localStorage.getItem('selected_lat'));
            const userLng = parseFloat(localStorage.getItem('selected_lng'));
            let filteredFoods = foods;
            
            if (userLat && userLng) {
                const restResponse = await fetch('/restaurants');
                if (restResponse.ok) {
                    const rests = await restResponse.json();
                    let nearbyRests = rests.filter(r => {
                        if (r.latitude && r.longitude) {
                            const dist = calculateDistance(userLat, userLng, r.latitude, r.longitude);
                            return dist <= 15.0;
                        }
                        return false;
                    });
                    
                    // If no restaurants are nearby, they will be teleported in the UI, so all are valid
                    const validRestIds = (nearbyRests.length > 0 ? nearbyRests : rests).map(r => r.id);
                    filteredFoods = foods.filter(f => validRestIds.includes(f.restaurantId));
                }
            }

            // Take first 4 items for trending
            const trending = filteredFoods.slice(0, 4);

            if (trending.length === 0) {
                container.innerHTML = `<p style="grid-column: 1/-1; text-align: center; color: var(--text-secondary);">No dishes available yet.</p>`;
                return;
            }

            container.innerHTML = trending.map(food => {
                const img = food.imageUrl || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=300&q=80';
                return `
                    <div class="card glass-panel">
                        <div class="card-img-wrapper">
                            <img src="${img}" alt="${food.name}">
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
                                <button class="btn btn-primary" onclick="addDishToCart(${food.id})">
                                    <i class="fas fa-shopping-basket"></i> Add
                                </button>
                            </div>
                        </div>
                    </div>
                `;
            }).join('');
        }
    } catch (e) {
        console.error("Failed to load trending dishes:", e);
        container.innerHTML = `<p style="grid-column: 1/-1; text-align: center; color: var(--danger);">Failed to load trending dishes.</p>`;
    }
}

async function addDishToCart(foodId) {
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

function handleHeroSearch() {
    const query = document.getElementById('hero-search-input').value.trim();
    if (query) {
        // Redirect to menus page if they search for foods, or restaurants page.
        // Let's redirect to restaurants.html with search param
        window.location.href = `restaurants.html?search=${encodeURIComponent(query)}`;
    } else {
        showAlert("Please enter a keyword to search!", "warning");
    }
}
