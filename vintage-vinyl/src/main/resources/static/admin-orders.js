$(document).ready(function() {
    window.setTimeout(function() {
        $('.alert').fadeTo(500, 0).slideUp(500, function(){
            $(this).remove();
        });
    }, 3000);

    $('form[action*="/delete"]').submit(function(e) {
        if (!confirm('Are you sure you want to delete this order?')) {
            e.preventDefault();
        }
    });
});