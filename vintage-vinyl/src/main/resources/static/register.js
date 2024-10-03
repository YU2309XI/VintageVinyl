$(document).ready(function() {
    // Simple form validation before submission
    $('form').submit(function(event) {
        var password = $('#password').val();
        var email = $('#email').val();

        // Check password length
        if (password.length < 6) {
            alert('Password must be at least 6 characters long');
            event.preventDefault();
        }

        // Basic email format validation using regex
        var emailPattern = /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/;
        if (!emailPattern.test(email)) {
            alert('Please enter a valid email address');
            event.preventDefault();
        }
    });
});
