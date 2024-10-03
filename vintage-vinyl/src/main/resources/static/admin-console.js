$(document).ready(function() {
    $('.make-admin-btn').click(function() {
        var username = $(this).data('username');
        if (confirm('Are you sure you want to make ' + username + ' an admin?')) {
            $.ajax({
                url: '/admin/set-admin',
                type: 'POST',
                data: { username: username },
                headers: {
                    'X-CSRF-TOKEN': $('meta[name="_csrf"]').attr('content')
                },
                success: function(response) {
                    alert('User ' + username + ' is now an admin.');
                    location.reload();
                },
                error: function(xhr, status, error) {
                    alert('An error occurred: ' + error);
                }
            });
        }
    });
});
