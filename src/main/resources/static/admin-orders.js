$(document).ready(function() {
    /**
     * Automatically hides alert messages after a specified time.
     *
     * This function will fade out alert elements with a smooth transition
     * and remove them from the DOM to prevent clutter.
     */
    window.setTimeout(function() {
        $('.alert').fadeTo(500, 0).slideUp(500, function() {
            $(this).remove();
        });
    }, 3000); // Alerts are displayed for 3 seconds before fading out

    /**
     * Confirmation dialog for deleting an order.
     *
     * Intercepts the form submission for delete actions and displays
     * a confirmation dialog to ensure the user wants to proceed.
     *
     * If the user cancels the confirmation, the form submission is prevented.
     */
    $('form[action*="/delete"]').submit(function(e) {
        if (!confirm('Are you sure you want to delete this order?')) {
            e.preventDefault(); // Prevents form submission if the user cancels
        }
    });
});
