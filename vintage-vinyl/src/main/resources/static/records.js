$(document).ready(function() {
    // CSRF token setup for AJAX requests
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");

    $(document).ajaxSend(function(e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });

    $('.add-to-cart').click(function() {
        var recordId = $(this).data('record-id');
        $.post('/cart/add', { recordId: recordId, quantity: 1 })
            .done(function() {
                alert('Record added to cart successfully!');
                var cartCountSpan = $('.fa-shopping-cart').next('span');
                var currentCount = parseInt(cartCountSpan.text());
                cartCountSpan.text(currentCount + 1);
            })
            .fail(function(xhr) {
                if (xhr.status === 401) {
                    window.location.href = '/login';
                } else {
                    alert('Failed to add record to cart. Please try again.');
                }
            });
    });

    $('.delete-record').click(function(e) {
        if (!confirm('Are you sure you want to delete this record? This action cannot be undone.')) {
            e.preventDefault();
        }
    });

    $('form[action="/wishlist/add"]').submit(function(e) {
        e.preventDefault();
        var form = $(this);
        $.post(form.attr('action'), form.serialize())
            .done(function() {
                alert('Record added to wishlist successfully!');
            })
            .fail(function(xhr) {
                if (xhr.status === 401) {
                    window.location.href = '/login';
                } else {
                    alert('Failed to add record to wishlist. Please try again.');
                }
            });
    });
});

