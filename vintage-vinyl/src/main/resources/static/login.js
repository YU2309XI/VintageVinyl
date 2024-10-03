$(document).ready(function() {
    $('form').submit(function(event) {
        var loginButton = $('button[type="submit"]');
        loginButton.prop('disabled', true).html('<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Logging in...');
    });
});
