$(document).ready(function() {
    // CSRF Token handling for AJAX requests
    const token = $("meta[name='_csrf']").attr("content");
    const header = $("meta[name='_csrf_header']").attr("content");

    // Set global AJAX defaults to include CSRF headers
    $.ajaxSetup({
        beforeSend: function(xhr) {
            xhr.setRequestHeader(header, token);
        }
    });

    /**
     * Display success or error messages to the user.
     *
     * @param {string} message - The message to display.
     * @param {boolean} [isError=false] - Whether the message is an error message.
     */
    function showMessage(message, isError = false) {
        const messageDiv = isError ? $("#errorMessage") : $("#successMessage");
        const otherDiv = isError ? $("#successMessage") : $("#errorMessage");

        messageDiv.text(message).show();
        otherDiv.hide();

        // Automatically hide the message after 3 seconds
        setTimeout(() => messageDiv.fadeOut(), 3000);
    }

    /**
     * Assign an admin role to a user.
     * Triggered by clicking the "Make Admin" button.
     */
    $('.make-admin-btn').click(function() {
        const username = $(this).data('username');
        if (confirm('Are you sure you want to make ' + username + ' an admin?')) {
            $.ajax({
                url: '/admin/set-admin',
                type: 'POST',
                data: { username: username },
                success: function(response) {
                    showMessage('User ' + username + ' is now an admin.');
                    location.reload();
                },
                error: function(xhr) {
                    showMessage(xhr.responseText, true);
                }
            });
        }
    });

    /**
     * Open the edit user modal and populate fields with user data.
     */
    $('.edit-user-btn').click(function() {
        const id = $(this).data('id');
        const username = $(this).data('username');
        const email = $(this).data('email');

        $('#editUserId').val(id);
        $('#editUsername').val(username);
        $('#editEmail').val(email);
        $('#editPassword').val(''); // Clear password field
        $('#editUserModal').modal('show');
    });

    /**
     * Save changes made to a user in the edit user modal.
     */
    $('#saveUserEdit').click(function() {
        const id = $('#editUserId').val();
        const userData = {
            username: $('#editUsername').val(),
            email: $('#editEmail').val(),
            password: $('#editPassword').val() || null
        };

        $.ajax({
            url: '/admin/users/' + id + '/edit',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(userData),
            success: function(response) {
                $('#editUserModal').modal('hide');
                showMessage('User updated successfully');
                location.reload();
            },
            error: function(xhr) {
                showMessage(xhr.responseText, true);
            }
        });
    });

    /**
     * Delete a user by clicking the deleted button.
     */
    $('.delete-user-btn').click(function() {
        const id = $(this).data('id');
        const username = $(this).data('username');

        if (confirm('Are you sure you want to delete user "' + username + '"? This action cannot be undone.')) {
            $.ajax({
                url: '/admin/users/' + id + '/delete',
                type: 'POST',
                success: function(response) {
                    showMessage('User deleted successfully');
                    location.reload();
                },
                error: function(xhr) {
                    showMessage(xhr.responseText, true);
                }
            });
        }
    });

    /**
     * Activate a user's account.
     */
    $('.activate-user-btn').click(function() {
        const id = $(this).data('id');
        $.ajax({
            url: '/admin/users/' + id + '/activate',
            type: 'POST',
            success: function(response) {
                showMessage('User activated successfully');
                location.reload();
            },
            error: function(xhr) {
                showMessage(xhr.responseText, true);
            }
        });
    });

    /**
     * Deactivate a user's account.
     */
    $('.deactivate-user-btn').click(function() {
        const id = $(this).data('id');
        if (confirm('Are you sure you want to deactivate this user?')) {
            $.ajax({
                url: '/admin/users/' + id + '/deactivate',
                type: 'POST',
                success: function(response) {
                    showMessage('User deactivated successfully');
                    location.reload();
                },
                error: function(xhr) {
                    showMessage(xhr.responseText, true);
                }
            });
        }
    });

    /**
     * Lock a user's account to prevent access.
     */
    $('.lock-user-btn').click(function() {
        const id = $(this).data('id');
        if (confirm('Are you sure you want to lock this user?')) {
            $.ajax({
                url: '/admin/users/' + id + '/lock',
                type: 'POST',
                success: function(response) {
                    showMessage('User locked successfully');
                    location.reload();
                },
                error: function(xhr) {
                    showMessage(xhr.responseText, true);
                }
            });
        }
    });

    /**
     * Unlock a user's account.
     */
    $('.unlock-user-btn').click(function() {
        const id = $(this).data('id');
        $.ajax({
            url: '/admin/users/' + id + '/unlock',
            type: 'POST',
            success: function(response) {
                showMessage('User unlocked successfully');
                location.reload();
            },
            error: function(xhr) {
                showMessage(xhr.responseText, true);
            }
        });
    });

    /**
     * Validate the edit user form before submission.
     */
    $('#editUserForm').on('submit', function(e) {
        e.preventDefault();

        const email = $('#editEmail').val();
        if (!email || !email.includes('@')) {
            showMessage('Please enter a valid email address', true);
            return false;
        }

        const password = $('#editPassword').val();
        if (password && password.length < 6) {
            showMessage('Password must be at least 6 characters long', true);
            return false;
        }

        $('#saveUserEdit').click();
    });

    /**
     * Handle AJAX errors globally for better user feedback.
     *
     * @param {Object} xhr - The XMLHttpRequest object containing error details.
     */
    function handleError(xhr) {
        let errorMessage;
        try {
            errorMessage = xhr.responseJSON ? xhr.responseJSON.message : xhr.responseText;
        } catch (e) {
            errorMessage = 'An unexpected error occurred';
        }
        showMessage(errorMessage, true);
    }
});
