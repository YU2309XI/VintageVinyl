$(document).ready(function() {
    // CSRF Token 处理
    const token = $("meta[name='_csrf']").attr("content");
    const header = $("meta[name='_csrf_header']").attr("content");

    // 设置全局 AJAX 默认值
    $.ajaxSetup({
        beforeSend: function(xhr) {
            xhr.setRequestHeader(header, token);
        }
    });

    // 消息显示函数
    function showMessage(message, isError = false) {
        const messageDiv = isError ? $("#errorMessage") : $("#successMessage");
        const otherDiv = isError ? $("#successMessage") : $("#errorMessage");

        messageDiv.text(message).show();
        otherDiv.hide();

        setTimeout(() => messageDiv.fadeOut(), 3000);
    }

    // 保留现有的设置管理员功能
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

    // 编辑用户功能
    $('.edit-user-btn').click(function() {
        const id = $(this).data('id');
        const username = $(this).data('username');
        const email = $(this).data('email');

        $('#editUserId').val(id);
        $('#editUsername').val(username);
        $('#editEmail').val(email);
        $('#editPassword').val(''); // 清空密码字段
        $('#editUserModal').modal('show');
    });

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

    // 删除用户功能
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

    // 激活用户功能
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

    // 停用用户功能
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

    // 锁定用户功能
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

    // 解锁用户功能
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

    // 表单验证
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

    // 错误处理函数
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