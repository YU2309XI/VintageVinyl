$(document).ready(function() {
    // Form validation on submission
    $('form').submit(function(event) {
        let isValid = true;

        if ($('#title').val().trim() === '') {
            isValid = false;
            alert('Please enter a title for the record.');
        }

        if ($('#artist').val().trim() === '') {
            isValid = false;
            alert('Please enter an artist name.');
        }

        const releaseYear = $('#releaseYear').val();
        const currentYear = new Date().getFullYear();
        if (releaseYear === '' || isNaN(releaseYear) || releaseYear < 1900 || releaseYear > currentYear) {
            isValid = false;
            alert('Please enter a valid release year (1900 - ' + currentYear + ').');
        }

        const price = $('#price').val();
        if (price === '' || isNaN(price) || price <= 0) {
            isValid = false;
            alert('Please enter a valid price (greater than 0).');
        }

        if (!isValid) {
            event.preventDefault();
        } else {
            if (!confirm('Are you sure you want to save this record?')) {
                event.preventDefault();
            }
        }
    });
});
