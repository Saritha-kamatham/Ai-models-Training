// ====================================================================
// COMMON FRONTEND APP UTILITIES (API Client, Session, Theme, Cart)
// ====================================================================

const API_BASE = ''; // Same domain for combined deployment

// Loader Utilities
function showLoader() {
    let loader = document.getElementById('global-loader');
    if (!loader) {
        loader = document.createElement('div');
        loader.id = 'global-loader';
        loader.className = 'loading-overlay';
        loader.innerHTML = '<div class="spinner"></div><p style="margin-top: 1rem; color: #fff; font-weight: 500;">Please wait...</p>';
        document.body.appendChild(loader);
    }
    loader.classList.add('active');
}

function hideLoader() {
    const loader = document.getElementById('global-loader');
    if (loader) {
        loader.classList.remove('active');
    }
}

// Alert/Toast Utility
function showAlert(message, type = 'info') {
    let alertContainer = document.getElementById('alert-container');
    if (!alertContainer) {
        alertContainer = document.createElement('div');
        alertContainer.id = 'alert-container';
        alertContainer.className = 'alert-container';
        document.body.appendChild(alertContainer);
    }

    const toast = document.createElement('div');
    toast.className = `alert-toast glass-panel alert-${type}`;
    
    let icon = 'info-circle';
    if (type === 'success') icon = 'check-circle';
    if (type === 'danger') icon = 'exclamation-circle';
    if (type === 'warning') icon = 'exclamation-triangle';

    toast.innerHTML = `<i class="fas fa-${icon}"></i> <span>${message}</span>`;
    alertContainer.appendChild(toast);

    // Auto remove toast after 3.5 seconds
    setTimeout(() => {
        toast.style.animation = 'slideOut 0.35s ease-in forwards';
        toast.addEventListener('animationend', () => {
            toast.remove();
        });
    }, 3500);
}

// Global API Fetch Interceptor
async function apiRequest(url, options = {}) {
    showLoader();
    options.headers = options.headers || {};
    
    if (!(options.body instanceof FormData)) {
        options.headers['Content-Type'] = options.headers['Content-Type'] || 'application/json';
    }
    
    const token = localStorage.getItem('jwt_token');
    if (token) {
        options.headers['Authorization'] = `Bearer ${token}`;
    }
    
    try {
        const response = await fetch(`${API_BASE}${url}`, options);
        hideLoader();
        
        if (response.status === 401) {
            localStorage.clear();
            showAlert('Session expired. Please log in again.', 'warning');
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 1500);
            return null;
        }

        // Return empty object on 204 or delete
        if (response.status === 204) {
            return {};
        }

        const contentType = response.headers.get("content-type");
        if (contentType && contentType.indexOf("application/json") !== -1) {
            const data = await response.json();
            if (!response.ok) {
                const errorMsg = data.message || Object.values(data).join(', ') || 'Operation failed';
                throw new Error(errorMsg);
            }
            return data;
        } else {
            // Text or HTML response
            const textData = await response.text();
            if (!response.ok) {
                throw new Error(textData || 'Operation failed');
            }
            return textData;
        }
    } catch (error) {
        hideLoader();
        showAlert(error.message, 'danger');
        throw error;
    }
}

// Session Helpers
const Session = {
    save(authData) {
        localStorage.setItem('jwt_token', authData.token);
        localStorage.setItem('user_email', authData.email);
        localStorage.setItem('user_role', authData.role);
        localStorage.setItem('user_name', authData.fullName);
        localStorage.setItem('user_id', authData.userId);
    },
    clear() {
        localStorage.clear();
    },
    isLoggedIn() {
        return localStorage.getItem('jwt_token') !== null;
    },
    getRole() {
        return localStorage.getItem('user_role');
    },
    getUsername() {
        return localStorage.getItem('user_name');
    },
    getUserId() {
        return localStorage.getItem('user_id');
    }
};

// Common Layout Renderer (Nav & Footer)
function renderNavbar() {
    const navContainer = document.querySelector('header');
    if (!navContainer) return;

    const isLoggedIn = Session.isLoggedIn();
    const role = Session.getRole();
    const username = Session.getUsername();
    const currentAddress = localStorage.getItem('selected_address') || 'Set Location';
    
    let dashboardLink = '';
    let ordersLink = '';
    let navActionsHtml = '';

    if (isLoggedIn) {
        if (role === 'ADMIN' || role === 'OWNER') {
            dashboardLink = `<li><a href="dashboard.html" id="nav-dashboard">Dashboard</a></li>`;
        }
        if (role === 'CUSTOMER') {
            ordersLink = `<li><a href="orders.html" id="nav-orders">My Orders</a></li>`;
        }
        
        navActionsHtml = `
            <span style="font-weight: 500; font-size: 0.95rem; margin-right: 0.5rem;">Hi, ${username} (${role})</span>
            <button class="btn btn-secondary" onclick="handleLogout()">Logout</button>
        `;
    } else {
        navActionsHtml = `
            <a href="login.html" class="btn btn-secondary">Login</a>
            <a href="register.html" class="btn btn-primary">Sign Up</a>
        `;
    }

    navContainer.innerHTML = `
        <nav class="navbar glass-panel">
            <div style="display: flex; align-items: center; gap: 1rem;">
                <div class="logo" onclick="window.location.href='index.html'">
                    <i class="fas fa-motorcycle"></i> FoodDash
                </div>
                <div class="location-selector" onclick="openMapModal()" style="display: flex; align-items: center; gap: 0.5rem; background: rgba(255,255,255,0.05); padding: 0.4rem 0.8rem; border-radius: 20px; border: 1px solid var(--glass-border);">
                    <i class="fas fa-map-marker-alt" style="color: var(--primary-color); font-size: 0.85rem;"></i>
                    <span id="current-location-text" style="color: var(--text-primary); font-weight: 600; font-size: 0.85rem; padding-right: 0.5rem; max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                        ${currentAddress}
                    </span>
                </div>
            </div>
            <ul class="nav-links">
                <li><a href="index.html" id="nav-home">Home</a></li>
                <li><a href="restaurants.html" id="nav-restaurants">Restaurants</a></li>
                <li><a href="menu.html" id="nav-menu">Explore Dishes</a></li>
                ${ordersLink}
                ${dashboardLink}
            </ul>
            <div class="nav-actions">
                <button class="theme-toggle" onclick="toggleTheme()">
                    <i class="fas fa-adjust"></i>
                </button>
                ${role === 'CUSTOMER' || !isLoggedIn ? `
                    <div class="cart-btn" onclick="window.location.href='cart.html'">
                        <i class="fas fa-shopping-cart"></i>
                        <span class="cart-badge" id="cart-badge-count">0</span>
                    </div>
                ` : ''}
                ${navActionsHtml}
            </div>
        </nav>
    `;
    
    // Set active link
    const path = window.location.pathname;
    const filename = path.substring(path.lastIndexOf('/') + 1);
    if (filename === 'index.html' || filename === '') {
        document.getElementById('nav-home')?.classList.add('active');
    } else if (filename === 'restaurants.html' || filename === 'restaurant-details.html') {
        document.getElementById('nav-restaurants')?.classList.add('active');
    } else if (filename === 'menu.html') {
        document.getElementById('nav-menu')?.classList.add('active');
    } else if (filename === 'orders.html') {
        document.getElementById('nav-orders')?.classList.add('active');
    } else if (filename === 'dashboard.html') {
        document.getElementById('nav-dashboard')?.classList.add('active');
    }

    // Load Cart Count
    if (isLoggedIn && role === 'CUSTOMER') {
        updateCartBadgeCount();
    }
}

function handleLogout() {
    Session.clear();
    showAlert('Logged out successfully!', 'success');
    setTimeout(() => {
        window.location.href = 'index.html';
    }, 1200);
}

// Cart badge counts update
async function updateCartBadgeCount() {
    const badge = document.getElementById('cart-badge-count');
    if (!badge || !Session.isLoggedIn() || Session.getRole() !== 'CUSTOMER') return;
    try {
        const response = await fetch('/cart', {
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('jwt_token')}`
            }
        });
        if (response.ok) {
            const cart = await response.json();
            const count = cart.items.reduce((total, item) => total + item.quantity, 0);
            badge.innerText = count;
            badge.style.display = count > 0 ? 'block' : 'none';
        }
    } catch (e) {
        console.error("Failed to update cart badge:", e);
    }
}

// Theme Management
function initTheme() {
    const savedTheme = localStorage.getItem('theme') || 'dark';
    document.documentElement.setAttribute('data-theme', savedTheme);
}

function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme');
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);
}

// Initializations
let appRestaurants = [];

async function loadAppRestaurantsForSuggestions() {
    try {
        const response = await fetch('/restaurants');
        if (response.ok) {
            appRestaurants = await response.json();
        }
    } catch (e) {
        console.error("Failed to load restaurants for suggestions:", e);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    renderNavbar();
    loadAppRestaurantsForSuggestions();
});

// ====================================================================
// LEAFLET MAP MODAL & DISTANCE UTILITIES
// ====================================================================
let leafletMap = null;
let leafletMarker = null;
let selectedLat = null;
let selectedLng = null;
let mapSearchDebounceTimer = null;

function handleMapSearchInput(val) {
    clearTimeout(mapSearchDebounceTimer);
    const query = val.trim();
    if (!query) {
        hideMapSuggestions();
        return;
    }
    
    mapSearchDebounceTimer = setTimeout(() => {
        showMapSuggestions(query);
    }, 300);
}

function hideMapSuggestions() {
    const container = document.getElementById('map-search-suggestions');
    if (container) {
        container.style.display = 'none';
        container.innerHTML = '';
    }
}

async function showMapSuggestions(query) {
    const container = document.getElementById('map-search-suggestions');
    if (!container) return;

    // 1. Filter local restaurants from database
    const matchedRestaurants = appRestaurants.filter(r => 
        r.name.toLowerCase().includes(query.toLowerCase()) ||
        r.cuisine.toLowerCase().includes(query.toLowerCase()) ||
        r.location.toLowerCase().includes(query.toLowerCase())
    );

    // Build restaurant suggestions html
    let itemsHtml = matchedRestaurants.map(r => `
        <div class="suggestion-item" onclick="selectSuggestion('restaurant', ${r.id}, '${r.name.replace(/'/g, "\\'")}', ${r.latitude}, ${r.longitude})">
            <i class="fas fa-utensils"></i>
            <div>
                <strong style="display: block;">${r.name}</strong>
                <span style="font-size: 0.75rem; color: var(--text-secondary);">${r.cuisine} &bull; ${r.location}</span>
            </div>
        </div>
    `).join('');

    // 2. Query OSM Nominatim address geocoding API
    try {
        const response = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&limit=5`);
        if (response.ok) {
            const places = await response.json();
            if (places && places.length > 0) {
                itemsHtml += places.map(p => {
                    const disp = p.display_name;
                    const mainName = disp.split(',')[0];
                    const details = disp.split(',').slice(1, 3).join(',').trim();
                    return `
                        <div class="suggestion-item" onclick="selectSuggestion('place', null, '${mainName.replace(/'/g, "\\'")}', ${p.lat}, ${p.lon})">
                            <i class="fas fa-map-marker-alt"></i>
                            <div>
                                <strong style="display: block;">${mainName}</strong>
                                <span style="font-size: 0.75rem; color: var(--text-secondary);">${details}</span>
                            </div>
                        </div>
                    `;
                }).join('');
            }
        }
    } catch (e) {
        console.error("OSM suggestion fetch error:", e);
    }

    if (itemsHtml) {
        container.innerHTML = itemsHtml;
        container.style.display = 'block';
    } else {
        container.innerHTML = `<div style="padding: 1rem; color: var(--text-secondary); text-align: center; font-size: 0.9rem;">No matches found</div>`;
        container.style.display = 'block';
    }
}

function selectSuggestion(type, id, name, lat, lng) {
    selectedLat = parseFloat(lat);
    selectedLng = parseFloat(lng);

    if (leafletMap) {
        leafletMap.setView([selectedLat, selectedLng], 14);
        if (leafletMarker) {
            leafletMarker.setLatLng([selectedLat, selectedLng]);
        }
    }

    document.getElementById('map-search-input').value = name;
    hideMapSuggestions();

    if (type === 'restaurant') {
        showAlert(`Selected Restaurant: ${name}`, "success");
    } else {
        showAlert(`Selected Location: ${name}`, "success");
    }
}

// Close suggestions on clicking outside
document.addEventListener('click', (e) => {
    const suggestions = document.getElementById('map-search-suggestions');
    const input = document.getElementById('map-search-input');
    if (suggestions && !suggestions.contains(e.target) && e.target !== input) {
        hideMapSuggestions();
    }
});

function openMapModal() {
    const modal = document.getElementById('map-modal');
    if (!modal) return;
    modal.classList.add('active');
    
    // Allow container to render before initializing Leaflet
    setTimeout(() => {
        initLeafletMap();
    }, 100);
}

function closeMapModal() {
    const modal = document.getElementById('map-modal');
    if (modal) modal.classList.remove('active');
}

function initLeafletMap() {
    const defaultLat = parseFloat(localStorage.getItem('selected_lat')) || 12.9716; // Bangalore default
    const defaultLng = parseFloat(localStorage.getItem('selected_lng')) || 77.5946;

    if (leafletMap) {
        leafletMap.invalidateSize();
        leafletMap.setView([defaultLat, defaultLng], 13);
        if (leafletMarker) {
            leafletMarker.setLatLng([defaultLat, defaultLng]);
        }
        return;
    }

    // Initialize map
    leafletMap = L.map('map-container').setView([defaultLat, defaultLng], 13);
    
    // Add OpenStreetMap tiles
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(leafletMap);

    // Place initial marker
    leafletMarker = L.marker([defaultLat, defaultLng], { draggable: true }).addTo(leafletMap);
    
    selectedLat = defaultLat;
    selectedLng = defaultLng;

    // Click on map to move marker
    leafletMap.on('click', (e) => {
        const { lat, lng } = e.latlng;
        selectedLat = lat;
        selectedLng = lng;
        leafletMarker.setLatLng([lat, lng]);
    });

    // Drag marker to update location
    leafletMarker.on('dragend', () => {
        const latlng = leafletMarker.getLatLng();
        selectedLat = latlng.lat;
        selectedLng = latlng.lng;
    });

    // Try browser geolocation to center on load if no custom location is set
    if (!localStorage.getItem('selected_lat') && navigator.geolocation) {
        navigator.geolocation.getCurrentPosition((pos) => {
            const userLat = pos.coords.latitude;
            const userLng = pos.coords.longitude;
            selectedLat = userLat;
            selectedLng = userLng;
            leafletMap.setView([userLat, userLng], 15);
            leafletMarker.setLatLng([userLat, userLng]);
        });
    }
}

async function confirmMapLocation() {
    if (!selectedLat || !selectedLng) {
        showAlert("Please select a location on the map!", "warning");
        return;
    }

    localStorage.setItem('selected_lat', selectedLat);
    localStorage.setItem('selected_lng', selectedLng);

    showLoader();
    const addressStr = await getAddressFromCoords(selectedLat, selectedLng);
    hideLoader();

    localStorage.setItem('selected_address', addressStr);

    // Update navbar UI text
    const locText = document.getElementById('current-location-text');
    if (locText) locText.innerText = addressStr;

    closeMapModal();
    showAlert(`Location set to: ${addressStr}`, "success");

    // Fire global event for content reload
    const event = new CustomEvent('locationChanged', {
        detail: { lat: selectedLat, lng: selectedLng, address: addressStr }
    });
    window.dispatchEvent(event);
}

async function searchMapLocation() {
    const query = document.getElementById('map-search-input').value.trim();
    if (!query) {
        showAlert("Please enter a town or city name to search!", "warning");
        return;
    }
    
    showLoader();
    try {
        const response = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&limit=1`);
        if (response.ok) {
            const data = await response.json();
            if (data && data.length > 0) {
                const lat = parseFloat(data[0].lat);
                const lon = parseFloat(data[0].lon);
                selectedLat = lat;
                selectedLng = lon;
                
                if (leafletMap) {
                    leafletMap.setView([lat, lon], 14);
                    if (leafletMarker) {
                        leafletMarker.setLatLng([lat, lon]);
                    }
                }
                showAlert(`Found: ${data[0].display_name.split(',')[0]}`, "success");
            } else {
                showAlert("Location not found! Please check spelling.", "error");
            }
        } else {
            showAlert("Search service unavailable.", "error");
        }
    } catch (e) {
        console.error("Nominatim search error:", e);
        showAlert("Search failed. Please try again.", "error");
    } finally {
        hideLoader();
    }
}

function handleMapSearchKeyPress(event) {
    if (event.key === 'Enter') {
        searchMapLocation();
    }
}

async function getAddressFromCoords(lat, lng) {
    try {
        const response = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`);
        if (response.ok) {
            const data = await response.json();
            const address = data.address;
            
            // Extract town/city/suburb
            const suburb = address.suburb || address.neighbourhood || address.locality || '';
            const city = address.city || address.town || address.village || '';
            
            if (suburb && city) {
                return `${suburb}, ${city}`;
            } else if (city) {
                return `${city}`;
            } else {
                return data.display_name.split(',').slice(0, 2).join(',') || "Pinned Location";
            }
        }
    } catch (e) {
        console.error("Nominatim reverse geocode error:", e);
    }
    return `Location (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
}

// Haversine Distance Formula in Kilometers
function calculateDistance(lat1, lon1, lat2, lon2) {
    if (!lat1 || !lon1 || !lat2 || !lon2) return null;
    const R = 6371; // Radius of the Earth in km
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = 
        Math.sin(dLat/2) * Math.sin(dLat/2) +
        Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * 
        Math.sin(dLon/2) * Math.sin(dLon/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c; // Distance in km
}
