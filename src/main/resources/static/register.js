$(document).ready(function() {
    /**
     * Simple form validation before submission
     *
     * Validates the email and password fields before allowing the form to be submitted.
     * Displays an alert if any validation fails and prevents the form from being submitted.
     */
    $('form').submit(function(event) {
        var password = $('#password').val(); // Retrieve the password field value
        var email = $('#email').val(); // Retrieve the email field value

        // Check if the password meets the minimum length requirement
        if (password.length < 6) {
            alert('Password must be at least 6 characters long'); // Show an error alert
            event.preventDefault(); // Prevent the form submission
        }

        // Basic email format validation using a regex pattern
        var emailPattern = /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/; // Regex for valid email
        if (!emailPattern.test(email)) {
            alert('Please enter a valid email address'); // Show an error alert
            event.preventDefault(); // Prevent the form submission
        }
    });
});
