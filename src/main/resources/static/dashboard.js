$(document).ready(function() {
    /**
     * Handle button click events for buttons with the class `btn-primary`.
     *
     * When a button is clicked, it is disabled to prevent multiple submissions
     * and its text is updated to indicate a loading state.
     */
    $('.btn-primary').click(function() {
        // Disable the button to prevent multiple clicks
        $(this).prop('disabled', true)
            // Change the button text to 'Loading...'
            .html('Loading...');
    });
});
