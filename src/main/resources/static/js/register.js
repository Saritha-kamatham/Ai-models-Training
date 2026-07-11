// ====================================================================
// REGISTER JS CONTROLLER
// ====================================================================

async function handleRegisterSubmit(event) {
    event.preventDefault();

    const fullName = document.getElementById('fullName').value.trim();
    const email = document.getElementById('email').value.trim();
    const phone = document.getElementById('phone').value.trim();
    const password = document.getElementById('password').value.trim();
    const role = document.getElementById('role').value;
    const address = document.getElementById('address').value.trim();
    const city = document.getElementById('city').value.trim();

    const nameError = document.getElementById('name-error');
    const emailError = document.getElementById('email-error');
    const phoneError = document.getElementById('phone-error');
    const passwordError = document.getElementById('password-error');

    // Reset errors
    nameError.style.display = 'none';
    emailError.style.display = 'none';
    phoneError.style.display = 'none';
    passwordError.style.display = 'none';

    let valid = true;

    if (fullName.length < 2) {
        nameError.innerText = "Name must be at least 2 characters.";
        nameError.style.display = 'block';
        valid = false;
    }

    if (!email.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
        emailError.innerText = "Please enter a valid email address.";
        emailError.style.display = 'block';
        valid = false;
    }

    if (!phone.match(/^[0-9]{10}$/)) {
        phoneError.innerText = "Phone number must be exactly 10 digits.";
        phoneError.style.display = 'block';
        valid = false;
    }

    if (password.length < 6) {
        passwordError.innerText = "Password must be at least 6 characters.";
        passwordError.style.display = 'block';
        valid = false;
    }

    if (!valid) return;

    try {
        const payload = {
            fullName,
            email,
            phone,
            password,
            role,
            address,
            city
        };

        const result = await apiRequest('/auth/register', {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        if (result) {
            showAlert('Registration successful! Please login.', 'success');
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 1500);
        }
    } catch (e) {
        console.error("Registration failed:", e);
        showAlert(e.message || "Registration failed. Try again.", "danger");
    }
}
