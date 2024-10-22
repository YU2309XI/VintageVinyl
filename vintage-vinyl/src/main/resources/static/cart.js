$(document).ready(function() {
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");

    $(document).ajaxSend(function(e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });

    function updateTotal() {
        var total = 0;
        $('.cart-item-row').each(function() {
            var quantity = parseInt($(this).find('.quantity-input').val());
            var price = parseFloat($(this).find('td').eq(2).data('price')); // 第3列是价格
            if (!isNaN(quantity) && !isNaN(price)) {
                total += quantity * price;
            }
        });
        $('#cart-total').text(total.toFixed(2));
    }

    function updateSubtotal($row) {
        var quantity = parseInt($row.find('.quantity-input').val());
        var price = parseFloat($row.find('td').eq(2).data('price')); // 第3列是价格
        if (!isNaN(quantity) && !isNaN(price)) {
            var subtotal = quantity * price;
            $row.find('.subtotal').text('$' + subtotal.toFixed(2));
        }
    }

    // 处理数量变更
    $('.quantity-input').on('change', function() {
        var $input = $(this);
        var recordId = $input.data('record-id');
        var quantity = parseInt($input.val());
        var $row = $input.closest('tr');

        // 验证数量
        if (quantity < 1) {
            $input.val(1);
            quantity = 1;
        }

        // 立即更新显示
        updateSubtotal($row);
        updateTotal();

        // 发送到服务器
        $.ajax({
            url: '/cart/update-quantity',
            type: 'POST',
            data: {
                recordId: recordId,
                quantity: quantity
            },
            success: function(response) {
                // 成功后不需要刷新页面
            },
            error: function() {
                alert('Failed to update cart');
                location.reload();
            }
        });
    });

    // 移除商品
    $('.remove-item').click(function(e) {
        e.preventDefault(); // 阻止表单提交
        var recordId = $(this).closest('form').find('input[name="recordId"]').val();

        $.post('/cart/remove', {
            recordId: recordId
        })
            .done(function() {
                location.reload();
            })
            .fail(function() {
                alert("Failed to remove item");
            });
    });

    // 清空购物车
    $('#clear-cart').click(function() {
        $.post('/cart/clear')
            .done(function() {
                location.reload();
            })
            .fail(function() {
                alert("Failed to clear cart");
            });
    });
});