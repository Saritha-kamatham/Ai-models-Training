// ====================================================================
// LOGIN JS CONTROLLER
// ====================================================================

function togglePasswordVisibility() {
    const passwordInput = document.getElementById('password');
    const toggleIcon = document.getElementById('toggle-password');
    
    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        toggleIcon.className = 'fas fa-eye password-toggle';
    } else {
        passwordInput.type = 'password';
        toggleIcon.className = 'fas fa-eye-slash password-toggle';
    }
}

async function handleLoginSubmit(event) {
    event.preventDefault();
    
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value.trim();
    
    const emailError = document.getElementById('email-error');
    const passwordError = document.getElementById('password-error');
    
    // Clear previous errors
    emailError.style.display = 'none';
    passwordError.style.display = 'none';
    
    let valid = true;
    
    // Simple checks
    if (!email.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
        emailError.innerText = "Please enter a valid email format.";
        emailError.style.display = 'block';
        valid = false;
    }
    
    if (password.length < 6) {
        passwordError.innerText = "Password must be at least 6 characters.";
        passwordError.style.display = 'block';
        valid = false;
    }
    
    if (!valid) return;
    
    try {
        const result = await apiRequest('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });
        
        if (result && result.token) {
            Session.save(result);
            showAlert('Login successful!', 'success');
            
            // Redirect based on role
            setTimeout(() => {
                if (result.role === 'ADMIN' || result.role === 'OWNER') {
                    window.location.href = 'dashboard.html';
                } else {
                    window.location.href = 'index.html';
                }
            }, 1000);
        }
    } catch (e) {
        console.error("Login failed:", e);
        showAlert(e.message || "Invalid credentials!", "danger");
    }
}
