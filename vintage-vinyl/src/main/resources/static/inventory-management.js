$(document).ready(function() {
    // CSRF setup
    const token = $("meta[name='_csrf']").attr("content");
    const header = $("meta[name='_csrf_header']").attr("content");

    $(document).ajaxSend(function(e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });

    // Periodic inventory status refresh
    function refreshInventoryStatus() {
        $.get('/admin/api/inventory/status')
            .done(function(data) {
                updateDashboardCards(data);
                updateLowStockAlerts(data.lowStockRecords);
            });
    }

    function updateDashboardCards(data) {
        $('.card-text').eq(0).text(data.totalItems);
        $('.card-text').eq(1).text(data.lowStockCount);
        $('.card-text').eq(2).text(data.outOfStockCount);
        $('.card-text').eq(3).text(data.totalStock);
    }

    // Refresh every 5 minutes
    setInterval(refreshInventoryStatus, 300000);

    // Single record stock update
    $('.update-stock').click(function() {
        const recordId = $(this).data('id');
        const title = $(this).data('title');
        const currentStock = $(this).data('current-stock');

        $('#recordId').val(recordId);
        $('#recordTitle').text(title);
        $('#newQuantity').val(currentStock);
        $('#stockUpdateModal').modal('show');
    });

    $('#saveStock').click(function() {
        const recordId = $('#recordId').val();
        const quantity = parseInt($('#newQuantity').val());

        if (quantity < 0) {
            alert('Stock quantity cannot be negative');
            return;
        }

        $.ajax({
            url: `/admin/api/records/${recordId}/stock`,
            method: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify({ quantity: quantity })
        })
            .done(function() {
                $('#stockUpdateModal').modal('hide');
                window.location.reload();
            })
            .fail(function(xhr) {
                alert('Failed to update stock: ' + xhr.responseText);
            });
    });

    // Batch update functionality
    $('#selectAll').change(function() {
        $('.record-select').prop('checked', $(this).is(':checked'));
        updateBatchUpdateButtonState();
    });

    $('.record-select').change(function() {
        updateBatchUpdateButtonState();
    });

    function updateBatchUpdateButtonState() {
        const selectedCount = $('.record-select:checked').length;
        $('#batchUpdateBtn').prop('disabled', selectedCount === 0)
            .text(`Batch Update (${selectedCount} selected)`);
    }

    $('#batchUpdateBtn').click(function() {
        const selectedRecords = $('.record-select:checked').map(function() {
            const $row = $(this).closest('tr');
            return {
                id: $(this).data('id'),
                title: $row.find('td:eq(1)').text(),
                currentStock: parseInt($row.find('td:eq(3)').text())
            };
        }).get();

        if (selectedRecords.length === 0) {
            alert('Please select records to update');
            return;
        }

        const tbody = $('#batchUpdateTable tbody');
        tbody.empty();

        selectedRecords.forEach(record => {
            tbody.append(`
                <tr>
                    <td>${record.title}</td>
                    <td>${record.currentStock}</td>
                    <td>
                        <input type="number" class="form-control form-control-sm batch-quantity" 
                               data-id="${record.id}" value="${record.currentStock}" min="0">
                    </td>
                </tr>
            `);
        });

        $('#batchUpdateModal').modal('show');
    });

    $('#saveBatchUpdate').click(function() {
        const updates = $('.batch-quantity').map(function() {
            return {
                id: $(this).data('id'),
                quantity: parseInt($(this).val())
            };
        }).get();

        if (updates.some(u => u.quantity < 0)) {
            alert('Stock quantities cannot be negative');
            return;
        }

        $.ajax({
            url: '/admin/api/records/stock/batch',
            method: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify(updates)
        })
            .done(function() {
                $('#batchUpdateModal').modal('hide');
                window.location.reload();
            })
            .fail(function(xhr) {
                alert('Failed to update stock: ' + xhr.responseText);
            });
    });

    // Low stock threshold update
    let thresholdUpdateTimeout;
    $('.threshold-input').change(function() {
        const $input = $(this);
        const recordId = $input.data('id');
        const newThreshold = parseInt($input.val());

        if (newThreshold < 0) {
            alert('Threshold cannot be negative');
            $input.val($input.attr('value'));
            return;
        }

        clearTimeout(thresholdUpdateTimeout);
        thresholdUpdateTimeout = setTimeout(() => {
            $.ajax({
                url: `/admin/api/records/${recordId}/threshold`,
                method: 'PUT',
                contentType: 'application/json',
                data: JSON.stringify({ threshold: newThreshold })
            })
                .done(function() {
                    $input.attr('value', newThreshold);
                })
                .fail(function(xhr) {
                    alert('Failed to update threshold: ' + xhr.responseText);
                    $input.val($input.attr('value'));
                });
        }, 500);
    });

    // Export inventory report
    window.exportInventory = function() {
        const data = [];
        $('#inventoryTable tbody tr').each(function() {
            const $row = $(this);
            data.push({
                title: $row.find('td:eq(1)').text(),
                artist: $row.find('td:eq(2)').text(),
                currentStock: $row.find('td:eq(3)').text(),
                threshold: $row.find('td:eq(4) input').val(),
                status: $row.find('td:eq(5)').text()
            });
        });

        // Convert to CSV
        let csv = 'Title,Artist,Current Stock,Low Stock Threshold,Status\n';
        data.forEach(item => {
            csv += `"${item.title}","${item.artist}",${item.currentStock},${item.threshold},"${item.status}"\n`;
        });

        // Create download link
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', 'inventory_report_' + new Date().toISOString().slice(0,10) + '.csv');
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    };

    // Search/filter functionality
    $('#inventorySearch').on('input', function() {
        const searchTerm = $(this).val().toLowerCase();
        $('#inventoryTable tbody tr').each(function() {
            const $row = $(this);
            const text = $row.find('td:eq(1), td:eq(2)').text().toLowerCase();
            $row.toggle(text.includes(searchTerm));
        });
    });

    // Initialize tooltips
    $('[data-toggle="tooltip"]').tooltip();

    // Initialize state
    updateBatchUpdateButtonState();
    refreshInventoryStatus();
});
