$(document).ready(function() {
    /**
     * Form validation on submission
     *
     * Ensures that all required fields are filled out with valid values
     * before allowing the form to be submitted.
     */
    $('form').submit(function(event) {
        let isValid = true;

        // Validate that the title field is not empty
        if ($('#title').val().trim() === '') {
            isValid = false;
            alert('Please enter a title for the record.');
        }

        // Validate that the artist field is not empty
        if ($('#artist').val().trim() === '') {
            isValid = false;
            alert('Please enter an artist name.');
        }

        // Validate release year: must be between 1900 and the current year
        const releaseDate = $('#releaseDate').val();
        const currentYear = new Date().getFullYear();
        if (releaseDate === '' || isNaN(releaseDate) || releaseDate < 1900 || releaseDate > currentYear) {
            isValid = false;
            alert('Please enter a valid release year (1900 - ' + currentYear + ').');
        }

        // Validate price: must be a positive number
        const price = $('#price').val();
        if (price === '' || isNaN(price) || price <= 0) {
            isValid = false;
            alert('Please enter a valid price (greater than 0).');
        }

        /**
         * New stock validations
         *
         * For new records, ensure the initial stock and low stock threshold
         * are valid numbers and meet the required conditions.
         */
        const initialStock = $('#initialStock');
        if (initialStock.length > 0) { // Only validate if the field exists (new records)
            const stockValue = initialStock.val();
            if (stockValue === '' || isNaN(stockValue) || parseInt(stockValue) < 0) {
                isValid = false;
                alert('Please enter a valid initial stock quantity (0 or greater).');
            }
        }

        // Validate a low stock threshold: must be 0 or greater
        const lowStockThreshold = $('#lowStockThreshold').val();
        if (lowStockThreshold === '' || isNaN(lowStockThreshold) || parseInt(lowStockThreshold) < 0) {
            isValid = false;
            alert('Please enter a valid low stock threshold (0 or greater).');
        }

        // Prevent form submission if any validation fails
        if (!isValid) {
            event.preventDefault();
        }
    });

    /**
     * Automatically update the stock status display when stock or threshold changes.
     *
     * Dynamically adjusts the status badge to reflect whether the stock level is:
     * - Out of Stock
     * - Low Stock
     * - In Stock
     */
    $('#initialStock, #lowStockThreshold').on('change', function() {
        updateStockStatus();
    });

    /**
     * Update the stock status display based on the stock and threshold values.
     */
    function updateStockStatus() {
        const stock = parseInt($('#initialStock').val()) || 0; // Default to 0 if empty or invalid
        const threshold = parseInt($('#lowStockThreshold').val()) || 5; // Default to 5 if empty or invalid

        const statusElement = $('.stock-status');
        if (statusElement.length) {
            if (stock === 0) {
                // Out of Stock
                statusElement.removeClass('bg-warning bg-success').addClass('bg-danger text-white');
                statusElement.find('span').text('Out of Stock');
            } else if (stock <= threshold) {
                // Low Stock
                statusElement.removeClass('bg-danger bg-success').addClass('bg-warning');
                statusElement.find('span').text('Low Stock');
            } else {
                // In Stock
                statusElement.removeClass('bg-danger bg-warning').addClass('bg-success text-white');
                statusElement.find('span').text('In Stock');
            }
        }
    }
});
