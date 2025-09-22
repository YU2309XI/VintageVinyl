$(document).ready(function() {
    /**
     * Automatically hide alert messages after 3 seconds.
     *
     * Fades out the alert messages with a smooth transition, slides them up,
     * and removes them from the DOM to keep the page clean.
     */
    window.setTimeout(function() {
        $('.alert').fadeTo(500, 0).slideUp(500, function() {
            $(this).remove();
        });
    }, 3000);

    /**
     * Handle the form submission for updating the order status.
     *
     * Checks if the new status is different from the current status.
     * If the status is being changed, a confirmation dialog is shown.
     * Prevents form submission if the user cancels the confirmation.
     */
    $('form[action*="/update-status"]').submit(function(e) {
        var newStatus = $('#status').val(); // Get the new status from the form
        var currentStatus = $('#status').attr('data-current-status'); // Get the current status from the attribute

        // Show confirmation dialog if the status is being updated
        if (newStatus !== currentStatus) {
            if (!confirm('Are you sure you want to update the order status to ' + newStatus + '?')) {
                e.preventDefault(); // Prevent form submission if the user cancels
                return false;
            }
        }
    });

    /**
     * Handle the form submission for canceling an order.
     *
     * Displays a confirmation dialog to ensure the user wants to cancel the order.
     * Prevents form submission if the user cancels the confirmation.
     */
    $('form[action*="/cancel"]').submit(function(e) {
        // Show confirmation dialog before canceling the order
        if (!confirm('Are you sure you want to cancel this order?')) {
            e.preventDefault(); // Prevent form submission if the user cancels
            return false;
        }
    });
});
