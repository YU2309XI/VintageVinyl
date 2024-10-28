$(document).ready(function() {
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");

    $(document).ajaxSend(function(e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });

    // Add to Cart functionality with stock check
    $('.add-to-cart').click(function() {
        var $button = $(this);
        var recordId = $button.data('record-id');

        $.get('/records/' + recordId + '/stock-status')
            .done(function(status) {
                if (status === 'Out of Stock') {
                    alert('Sorry, this item is currently out of stock.');
                    $button.prop('disabled', true).html('<i class="fa fa-shopping-cart"></i> Out of Stock');
                    return;
                }

                // 如果有库存，继续添加到购物车
                $button.prop('disabled', true)
                    .html('<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Adding...');

                $.post('/cart/add', { recordId: recordId, quantity: 1 })
                    .done(function() {
                        alert('Record added to cart successfully!');
                        var cartCountSpan = $('.fa-shopping-cart').next('span');
                        var currentCount = parseInt(cartCountSpan.text());
                        cartCountSpan.text(currentCount + 1);
                    })
                    .fail(function(xhr) {
                        if (xhr.status === 400) {
                            alert('Not enough stock available.');
                        } else {
                            alert('Failed to add record to cart.');
                        }
                    })
                    .always(function() {
                        $button.prop('disabled', false)
                            .html('<i class="fa fa-shopping-cart"></i> Add to Cart');
                    });
            })
            .fail(function() {
                alert('Failed to check stock status.');
            });
    });

    // Confirm record deletion
    $('.delete-record').click(function(e) {
        if (!confirm('Are you sure you want to delete this record? This action cannot be undone.')) {
            e.preventDefault();
        }
    });

    // Auto refresh stock status every 30 seconds
    function refreshStockStatus() {
        var recordId = $('.add-to-cart').data('record-id');

        $.get('/records/' + recordId + '/stock-status')
            .done(function(status) {
                var $badge = $('.stock-info .badge');
                var $stockCount = $('.stock-info small');
                var $addToCartBtn = $('.add-to-cart');

                // Update badge status and color
                $badge.removeClass('badge-success badge-warning badge-danger')
                    .addClass(status === 'Out of Stock' ? 'badge-danger' :
                        (status === 'Low Stock' ? 'badge-warning' : 'badge-success'))
                    .text(status);

                // Update add to cart button
                if (status === 'Out of Stock') {
                    $addToCartBtn.prop('disabled', true)
                        .html('<i class="fa fa-shopping-cart"></i> Out of Stock');
                } else {
                    $addToCartBtn.prop('disabled', false)
                        .html('<i class="fa fa-shopping-cart"></i> Add to Cart');
                }
            });
    }

    setInterval(refreshStockStatus, 30000);
});