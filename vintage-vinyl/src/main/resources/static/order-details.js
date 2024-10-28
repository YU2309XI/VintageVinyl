$(document).ready(function() {
    window.setTimeout(function() {
        $('.alert').fadeTo(500, 0).slideUp(500, function(){
            $(this).remove();
        });
    }, 3000);

    $('form[action*="/update-status"]').submit(function(e) {
        var newStatus = $('#status').val();
        var currentStatus = $('#status').attr('data-current-status');

        if (newStatus !== currentStatus) {
            if (!confirm('Are you sure you want to update the order status to ' + newStatus + '?')) {
                e.preventDefault();
                return false;
            }
        }
    });

    $('form[action*="/cancel"]').submit(function(e) {
        if (!confirm('Are you sure you want to cancel this order?')) {
            e.preventDefault();
            return false;
        }
    });
});