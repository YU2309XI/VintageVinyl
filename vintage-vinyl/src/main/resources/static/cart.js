$(document).ready(function() {
    // Retrieve CSRF token and header from meta-tags for secure AJAX requests
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");

    // Attach CSRF token to all AJAX requests
    $(document).ajaxSend(function(e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });

    /**
     * Update the total price displayed in the shopping cart.
     *
     * Loops through all cart items, calculates the total based on the
     * quantity and price, and updates the total display.
     */
    function updateTotal() {
        var total = 0;
        $('.cart-item-row').each(function() {
            var quantity = parseInt($(this).find('.quantity-input').val());
            var price = parseFloat($(this).find('td').eq(2).data('price')); // Price is in the 3rd column
            if (!isNaN(quantity) && !isNaN(price)) {
                total += quantity * price;
            }
        });
        $('#cart-total').text(total.toFixed(2)); // Update the total with two decimal places
    }

    /**
     * Update the subtotal for a specific cart item.
     *
     * Calculates the subtotal for the item based on its quantity and price
     * and updates the corresponding display.
     *
     * @param {Object} $row - The jQuery object for the cart item row.
     */
    function updateSubtotal($row) {
        var quantity = parseInt($row.find('.quantity-input').val());
        var price = parseFloat($row.find('td').eq(2).data('price')); // Price is in the 3rd column
        if (!isNaN(quantity) && !isNaN(price)) {
            var subtotal = quantity * price;
            $row.find('.subtotal').text('$' + subtotal.toFixed(2)); // Display the subtotal
        }
    }

    /**
     * Handle quantity changes for cart items.
     *
     * Updates the subtotal and total prices dynamically and sends an AJAX
     * request to update the quantity on the server.
     */
    $('.quantity-input').on('change', function() {
        var $input = $(this);
        var recordId = $input.data('record-id');
        var quantity = parseInt($input.val());
        var $row = $input.closest('tr');

        // Ensure quantity is at least 1
        if (quantity < 1) {
            $input.val(1);
            quantity = 1;
        }

        // Update subtotal and total
        updateSubtotal($row);
        updateTotal();

        // Send AJAX request to update quantity on the server
        $.ajax({
            url: '/cart/update-quantity',
            type: 'POST',
            data: {
                recordId: recordId,
                quantity: quantity
            },
            success: function(response) {
                // No need to reload the page on success
            },
            error: function() {
                alert('Failed to update cart');
                location.reload(); // Reload the page if the update fails
            }
        });
    });

    /**
     * Remove an item from the cart.
     *
     * Sends an AJAX request to remove the item and reloads the page on success.
     */
    $('.remove-item').click(function(e) {
        e.preventDefault(); // Prevent form submission
        var recordId = $(this).closest('form').find('input[name="recordId"]').val();

        $.post('/cart/remove', {
            recordId: recordId
        })
            .done(function() {
                location.reload(); // Reload the page on successful removal
            })
            .fail(function() {
                alert("Failed to remove item");
            });
    });

    /**
     * Clear all items from the cart.
     *
     * Sends an AJAX request to clear the cart and reloads the page on success.
     */
    $('#clear-cart').click(function() {
        $.post('/cart/clear')
            .done(function() {
                location.reload(); // Reload the page on successful cart clearance
            })
            .fail(function() {
                alert("Failed to clear cart");
            });
    });
});
