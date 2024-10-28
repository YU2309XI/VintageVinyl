$(document).ready(function() {
    // Form validation on submission
    $('form').submit(function(event) {
        let isValid = true;

        // Existing validations
        if ($('#title').val().trim() === '') {
            isValid = false;
            alert('Please enter a title for the record.');
        }

        if ($('#artist').val().trim() === '') {
            isValid = false;
            alert('Please enter an artist name.');
        }

        const releaseDate = $('#releaseDate').val();
        const currentYear = new Date().getFullYear();
        if (releaseDate === '' || isNaN(releaseDate) || releaseDate < 1900 || releaseDate > currentYear) {
            isValid = false;
            alert('Please enter a valid release year (1900 - ' + currentYear + ').');
        }

        const price = $('#price').val();
        if (price === '' || isNaN(price) || price <= 0) {
            isValid = false;
            alert('Please enter a valid price (greater than 0).');
        }

        // New stock validations
        const initialStock = $('#initialStock');
        if (initialStock.length > 0) { // Only for new records
            const stockValue = initialStock.val();
            if (stockValue === '' || isNaN(stockValue) || parseInt(stockValue) < 0) {
                isValid = false;
                alert('Please enter a valid initial stock quantity (0 or greater).');
            }
        }

        const lowStockThreshold = $('#lowStockThreshold').val();
        if (lowStockThreshold === '' || isNaN(lowStockThreshold) || parseInt(lowStockThreshold) < 0) {
            isValid = false;
            alert('Please enter a valid low stock threshold (0 or greater).');
        }

        if (!isValid) {
            event.preventDefault();
        }
    });

    // Auto-update stock status display
    $('#initialStock, #lowStockThreshold').on('change', function() {
        updateStockStatus();
    });

    function updateStockStatus() {
        const stock = parseInt($('#initialStock').val()) || 0;
        const threshold = parseInt($('#lowStockThreshold').val()) || 5;

        const statusElement = $('.stock-status');
        if (statusElement.length) {
            if (stock === 0) {
                statusElement.removeClass('bg-warning bg-success').addClass('bg-danger text-white');
                statusElement.find('span').text('Out of Stock');
            } else if (stock <= threshold) {
                statusElement.removeClass('bg-danger bg-success').addClass('bg-warning');
                statusElement.find('span').text('Low Stock');
            } else {
                statusElement.removeClass('bg-danger bg-warning').addClass('bg-success text-white');
                statusElement.find('span').text('In Stock');
            }
        }
    }
});