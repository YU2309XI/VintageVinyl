$(document).ready(function() {
    /**
     * CSRF Token setup
     *
     * Configures all outgoing AJAX requests to include the CSRF token for security.
     */
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");

    $(document).ajaxSend(function(e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });

    /**
     * Add to Cart functionality with stock check
     *
     * Verifies stock availability before adding the item to the cart.
     * Provides feedback through button state changes and alerts.
     */
    $('.add-to-cart').click(function() {
        var $button = $(this);
        var recordId = $button.data('record-id');

        // Check stock status
        $.get('/records/' + recordId + '/stock-status')
            .done(function(status) {
                if (status === 'Out of Stock') {
                    alert('Sorry, this item is currently out of stock.');
                    $button.prop('disabled', true)
                        .html('<i class="fa fa-shopping-cart"></i> Out of Stock');
                    return;
                }

                // Proceed to add item to cart
                $button.prop('disabled', true)
                    .html('<span class="spinner-border spinner-border-sm"></span> Adding...');

                $.post('/cart/add', { recordId: recordId, quantity: 1 })
                    .done(function() {
                        alert('Record added to cart successfully!');
                        updateCartCount(1); // Increment cart count
                    })
                    .fail(function(xhr) {
                        if (xhr.status === 401) {
                            window.location.href = '/login'; // Redirect if not logged in
                        } else if (xhr.status === 400) {
                            alert('Not enough stock available.');
                        } else {
                            alert('Failed to add record to cart. Please try again.');
                        }
                    })
                    .always(function() {
                        $button.prop('disabled', false)
                            .html('<i class="fa fa-shopping-cart"></i> Add to Cart');
                    });
            });
    });

    /**
     * Quick Stock Update functionality
     *
     * Opens a modal for updating the stock of a specific record and
     * sends the updated stock value to the server.
     */
    $('.quick-stock-update').click(function() {
        var recordId = $(this).data('record-id');
        var currentStock = $(this).data('current-stock');
        $('#updateRecordId').val(recordId);
        $('#newStockQuantity').val(currentStock);
        $('#quickStockModal').modal('show');
    });

    $('#confirmStockUpdate').click(function() {
        var recordId = $('#updateRecordId').val();
        var newQuantity = $('#newStockQuantity').val();

        $.ajax({
            url: '/admin/api/records/' + recordId + '/stock',
            method: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify({ quantity: parseInt(newQuantity) })
        })
            .done(function() {
                $('#quickStockModal').modal('hide');
                window.location.reload(); // Reload the page to reflect changes
            })
            .fail(function(xhr) {
                alert('Failed to update stock: ' + xhr.responseText);
            });
    });

    /**
     * Delete confirmation
     *
     * Displays a confirmation dialog before allowing the deletion of a record.
     */
    $('.delete-record').click(function(e) {
        if (!confirm('Are you sure you want to delete this record? This action cannot be undone.')) {
            e.preventDefault(); // Cancel the delete action if not confirmed
        }
    });

    /**
     * Wishlist functionality
     *
     * Handles adding records to the wishlist with feedback on success or failure.
     */
    $('form[action="/wishlist/add"]').submit(function(e) {
        e.preventDefault();
        var form = $(this);

        $.post(form.attr('action'), form.serialize())
            .done(function() {
                alert('Record added to wishlist successfully!');
            })
            .fail(function(xhr) {
                if (xhr.status === 401) {
                    window.location.href = '/login'; // Redirect if not logged in
                } else {
                    alert('Failed to add record to wishlist. Please try again.');
                }
            });
    });

    /**
     * Helper function to update the cart count
     *
     * @param {number} increment - The number to increment the cart count by.
     */
    function updateCartCount(increment) {
        var cartCountSpan = $('.fa-shopping-cart').next('span');
        var currentCount = parseInt(cartCountSpan.text());
        cartCountSpan.text(currentCount + increment);
    }

    /**
     * Periodic stock status refresh
     *
     * Updates the stock status of each record and adjusts the UI elements
     * (badges and buttons) accordingly.
     */
    function refreshStockStatus() {
        $('.add-to-cart').each(function() {
            var $button = $(this);
            var recordId = $button.data('record-id');
            var $row = $button.closest('tr');
            var $statusCell = $row.find('td:nth-last-child(2)');

            $.get('/records/' + recordId + '/stock-status')
                .done(function(status) {
                    // Update status badge
                    var badgeClass = status === 'Out of Stock' ? 'badge-danger' :
                        (status === 'Low Stock' ? 'badge-warning' : 'badge-success');
                    $statusCell.find('.badge')
                        .removeClass('badge-success badge-warning badge-danger')
                        .addClass(badgeClass)
                        .text(status);

                    // Update Add to Cart button
                    $button.prop('disabled', status === 'Out of Stock')
                        .html('<i class="fa fa-shopping-cart"></i> ' +
                            (status === 'Out of Stock' ? 'Out of Stock' : 'Add to Cart'));
                });
        });
    }

    // Refresh stock status every 30 seconds
    setInterval(refreshStockStatus, 30000);
});
