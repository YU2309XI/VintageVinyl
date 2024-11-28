$(document).ready(function() {
    /**
     * Handle form submission to disable the submitted button and show a loading spinner.
     *
     * This ensures that users cannot submit the form multiple times and provides
     * a visual indicator that the login process is in progress.
     */
    $('form').submit(function(event) {
        // Select the submitted button within the form
        var loginButton = $('button[type="submit"]');

        // Disable the button to prevent multiple submissions
        loginButton.prop('disabled', true)
            // Update the button text to show a spinner and a loading message
            .html('<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Logging in...');
    });
});
