$(document).ready(function() {
    // CSRF Token setup for secure AJAX requests
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");

    // Attach CSRF token to all outgoing AJAX requests
    $(document).ajaxSend(function(e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });

    /**
     * Add to Cart functionality with stock check
     *
     * Checks the stock status of the item before adding it to the cart.
     * Updates the button state and provides feedback to the user.
     */
    $('.add-to-cart').click(function() {
        var $button = $(this);
        var recordId = $button.data('record-id');

        // Check the stock status before adding to cart
        $.get('/records/' + recordId + '/stock-status')
            .done(function(status) {
                if (status === 'Out of Stock') {
                    // Handle out-of-stock items
                    alert('Sorry, this item is currently out of stock.');
                    $button.prop('disabled', true).html('<i class="fa fa-shopping-cart"></i> Out of Stock');
                    return;
                }

                // If in stock, proceed to add the item to the cart
                $button.prop('disabled', true)
                    .html('<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Adding...');

                $.post('/cart/add', { recordId: recordId, quantity: 1 })
                    .done(function() {
                        // Successfully added to cart
                        alert('Record added to cart successfully!');
                        var cartCountSpan = $('.fa-shopping-cart').next('span');
                        var currentCount = parseInt(cartCountSpan.text());
                        cartCountSpan.text(currentCount + 1); // Update cart count
                    })
                    .fail(function(xhr) {
                        // Handle errors during the addition process
                        if (xhr.status === 400) {
                            alert('Not enough stock available.');
                        } else {
                            alert('Failed to add record to cart.');
                        }
                    })
                    .always(function() {
                        // Reset the button state
                        $button.prop('disabled', false)
                            .html('<i class="fa fa-shopping-cart"></i> Add to Cart');
                    });
            })
            .fail(function() {
                // Handle failure to check stock status
                alert('Failed to check stock status.');
            });
    });

    /**
     * Confirm record deletion
     *
     * Displays a confirmation dialog to prevent accidental deletions.
     */
    $('.delete-record').click(function(e) {
        if (!confirm('Are you sure you want to delete this record? This action cannot be undone.')) {
            e.preventDefault(); // Cancel the delete action if not confirmed
        }
    });

    /**
     * Auto-refresh stock status every 30 seconds
     *
     * Periodically checks the stock status of the item and updates
     * the UI elements such as badges and buttons accordingly.
     */
    function refreshStockStatus() {
        var recordId = $('.add-to-cart').data('record-id');

        $.get('/records/' + recordId + '/stock-status')
            .done(function(status) {
                var $badge = $('.stock-info .badge'); // Badge displaying stock status
                var $stockCount = $('.stock-info small'); // Stock count (if applicable)
                var $addToCartBtn = $('.add-to-cart'); // Add to Cart button

                // Update badge status and color
                $badge.removeClass('badge-success badge-warning badge-danger')
                    .addClass(status === 'Out of Stock' ? 'badge-danger' :
                        (status === 'Low Stock' ? 'badge-warning' : 'badge-success'))
                    .text(status);

                // Update the Add to Cart button based on stock status
                if (status === 'Out of Stock') {
                    $addToCartBtn.prop('disabled', true)
                        .html('<i class="fa fa-shopping-cart"></i> Out of Stock');
                } else {
                    $addToCartBtn.prop('disabled', false)
                        .html('<i class="fa fa-shopping-cart"></i> Add to Cart');
                }
            });
    }

    // Refresh stock status every 30 seconds
    setInterval(refreshStockStatus, 30000);
});
