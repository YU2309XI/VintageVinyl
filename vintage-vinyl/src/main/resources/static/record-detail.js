$(document).ready(function() {
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");

    $(document).ajaxSend(function(e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });

    // Add to Cart functionality
    $('.add-to-cart').click(function() {
        var recordId = $(this).data('record-id');
        $(this).prop('disabled', true).html('<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Adding...');

        $.post('/cart/add', { recordId: recordId, quantity: 1 })
            .done(function() {
                alert('Record added to cart successfully!');
                var cartCountSpan = $('.fa-shopping-cart').next('span');
                var currentCount = parseInt(cartCountSpan.text());
                cartCountSpan.text(currentCount + 1);
            })
            .fail(function(xhr) {
                alert('Failed to add record to cart.');
            })
            .always(function() {
                $('.add-to-cart').prop('disabled', false).html('Add to Cart');
            });
    });

    // Confirm record deletion
    $('.delete-record').click(function(e) {
        if (!confirm('Are you sure you want to delete this record? This action cannot be undone.')) {
            e.preventDefault();
        }
    });
});
