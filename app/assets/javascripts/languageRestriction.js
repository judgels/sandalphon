$(document).ready(function() {
    $('#isAllowedAll').on('click', function() {
        if ($('#isAllowedAll').prop('checked')) {
            $('.allowedLanguageName').each(function() {
                $(this).prop('disabled', true);
                $(this).prop('checked', true);
            });
        } else {
            $('.allowedLanguageName').each(function() {
                $(this).prop('disabled', false);
            });
        }
    });
});
