$(document).ready(function() {
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");

    $(document).ajaxSend(function(e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });

    function updateCartTotal() {
        $.get('/cart/total', function(total) {
            $('#cart-total').text('$' + total.toFixed(2));
        });
    }

    // Update cart quantity
    $('.quantity-input').change(function() {
        var recordId = $(this).data('record-id');
        var quantity = $(this).val();

        $.post('/cart/add', { recordId: recordId, quantity: quantity })
            .done(function() {
                updateCartTotal();
            })
            .fail(function(xhr, status, error) {
                alert('Failed to update cart. Please try again.');
            });
    });

    // Remove item from cart
    $('.remove-item').click(function() {
        var recordId = $(this).data('record-id');
        $.ajax({
            url: '/cart/remove',
            type: 'POST',
            data: { recordId: recordId },
            success: function(response) {
                location.reload();
            },
            error: function(xhr, status, error) {
                alert("Failed to remove item from cart. Please try again.");
            }
        });
    });

    // Clear cart
    $('#clear-cart').click(function() {
        $.ajax({
            url: '/cart/clear',
            type: 'POST',
            success: function(response) {
                location.reload();
            },
            error: function(xhr, status, error) {
                alert("Failed to clear cart. Please try again.");
            }
        });
    });

    updateCartTotal();
});
