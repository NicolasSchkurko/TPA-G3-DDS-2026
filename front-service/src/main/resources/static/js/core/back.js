(function () {
    document.addEventListener("click", function (event) {
        var link = event.target.closest("[data-back]");
        if (!link || event.defaultPrevented || event.button !== 0) return;
        if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;

        var referrer = document.referrer;
        var sameOriginReferrer = false;
        try {
            sameOriginReferrer = referrer && new URL(referrer).origin === window.location.origin;
        } catch (error) {
            sameOriginReferrer = false;
        }

        if (!sameOriginReferrer || window.history.length <= 1) return;
        event.preventDefault();
        window.history.back();
    });
})();
