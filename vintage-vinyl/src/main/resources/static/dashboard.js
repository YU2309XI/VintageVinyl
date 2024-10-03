$(document).ready(function() {
    $('.btn-primary').click(function() {
        $(this).prop('disabled', true).html('Loading...');
    });
});
