// ====================================================================
// RESTAURANT LISTING CONTROLLER
// ====================================================================

let allRestaurants = [];
let displayedRestaurants = [];
let activeCuisineFilter = 'ALL';
let ratingSortAsc = false;

document.addEventListener('DOMContentLoaded', async () => {
    // Check if redirect search parameter exists
    const urlParams = new URLSearchParams(window.location.search);
    const searchParam = urlParams.get('search');
    
    // Listen to global location changes
    window.addEventListener('locationChanged', applyFilters);
    
    await fetchRestaurants();
    
    if (searchParam) {
        document.getElementById('restaurant-search-input').value = searchParam;
        performSearch(searchParam);
    }
});

async function fetchRestaurants() {
    const grid = document.getElementById('restaurants-grid');
    if (!grid) return;

    try {
        const response = await fetch('/restaurants');
        if (response.ok) {
            allRestaurants = await response.json();
            applyFilters();
        }
    } catch (e) {
        console.error("Fetch restaurants error:", e);
        grid.innerHTML = `<p style="grid-column: 1/-1; text-align: center; color: var(--danger);">Failed to load restaurants.</p>`;
    }
}

function renderRestaurantsGrid() {
    const grid = document.getElementById('restaurants-grid');
    if (!grid) return;

    if (displayedRestaurants.length === 0) {
        const hasLocation = localStorage.getItem('selected_lat') !== null;
        const msg = hasLocation 
            ? "No restaurants found within delivery range (15 km) from your pinned location. Please click 'Set Location' in the navbar to pin a location in Bangalore or Mumbai centers!"
            : "No restaurants found matching your criteria.";
        grid.innerHTML = `<p style="grid-column: 1/-1; text-align: center; color: var(--text-secondary); line-height: 1.6; max-width: 500px; margin: 2rem auto;">${msg}</p>`;
        return;
    }

    grid.innerHTML = displayedRestaurants.map(restaurant => {
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

function handleSearchInput(event) {
    const query = event.target.value.trim();
    performSearch(query);
}

function performSearch(query) {
    if (!query) {
        applyFilters();
        return;
    }
    
    // Client-side search in name, cuisine, and location
    displayedRestaurants = allRestaurants.filter(r => 
        r.name.toLowerCase().includes(query.toLowerCase()) ||
        r.cuisine.toLowerCase().includes(query.toLowerCase()) ||
        r.location.toLowerCase().includes(query.toLowerCase())
    );
    
    if (activeCuisineFilter !== 'ALL') {
        displayedRestaurants = displayedRestaurants.filter(r => 
            r.cuisine.toLowerCase().includes(activeCuisineFilter.toLowerCase())
        );
    }
    
    renderRestaurantsGrid();
}

function filterByCuisine(cuisine) {
    activeCuisineFilter = cuisine;
    
    // Update active UI chips
    const chips = document.querySelectorAll('.filter-chip');
    chips.forEach(chip => chip.classList.remove('active'));
    
    if (cuisine === 'ALL') {
        document.getElementById('chip-all').classList.add('active');
    } else if (cuisine.toLowerCase().includes('italian')) {
        document.getElementById('chip-italian').classList.add('active');
    } else if (cuisine.toLowerCase().includes('burger')) {
        document.getElementById('chip-burgers').classList.add('active');
    } else if (cuisine.toLowerCase().includes('japan')) {
        document.getElementById('chip-japanese').classList.add('active');
    } else if (cuisine.toLowerCase().includes('mexican')) {
        document.getElementById('chip-mexican').classList.add('active');
    }
    
    applyFilters();
}

function applyFilters() {
    const query = document.getElementById('restaurant-search-input').value.trim();
    
    const userLat = parseFloat(localStorage.getItem('selected_lat'));
    const userLng = parseFloat(localStorage.getItem('selected_lng'));
    
    let result = allRestaurants.map(r => {
        if (userLat && userLng && r.latitude && r.longitude) {
            r.distance = calculateDistance(userLat, userLng, r.latitude, r.longitude);
        } else {
            r.distance = null;
        }
        return r;
    });
    
    // Filter by distance: If user has pinned a location, check if any are within 15 km
    if (userLat && userLng) {
        let nearby = result.filter(r => r.distance !== null && r.distance <= 15.0);
        
        // If NO restaurants are within 15 km, dynamically "teleport" the restaurants
        // to be nearby (center around user's location) so they can order food from anywhere!
        if (nearby.length === 0) {
            const address = localStorage.getItem('selected_address') || '';
            const townName = address.split(',')[0].trim() || 'Local';
            result = result.map((r, index) => {
                if (!r.originalName) r.originalName = r.name;
                if (!r.originalLocation) r.originalLocation = r.location;

                const angle = (index * 2 * Math.PI) / result.length;
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
            // Restore original name/location for nearby items
            result = nearby.map(r => {
                if (r.originalName) r.name = r.originalName;
                if (r.originalLocation) r.location = r.originalLocation;
                return r;
            });
        }
        // Sort by distance (closest first)
        result.sort((a, b) => a.distance - b.distance);
    }
    
    if (activeCuisineFilter !== 'ALL') {
        result = result.filter(r => r.cuisine.toLowerCase().includes(activeCuisineFilter.toLowerCase()));
    }
    
    if (query) {
        result = result.filter(r => 
            r.name.toLowerCase().includes(query.toLowerCase()) ||
            r.cuisine.toLowerCase().includes(query.toLowerCase()) ||
            r.location.toLowerCase().includes(query.toLowerCase())
        );
    }
    
    displayedRestaurants = result;
    renderRestaurantsGrid();
}

function sortByRating() {
    displayedRestaurants.sort((a, b) => b.rating - a.rating);
    renderRestaurantsGrid();
    showAlert("Sorted by Top Rated!", "success");
}
