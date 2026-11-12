/* =========================================================
   DDSI · Polling ligero con pausa cuando la pestaña no está visible
   ========================================================= */
(function () {
    "use strict";

    /**
     * Ejecuta `fn()` cada `intervaloMs`. Se pausa automáticamente
     * cuando la pestaña no está visible y reanuda al volver.
     * Devuelve una función para detener el polling.
     */
    function watch(fn, intervaloMs) {
        let timer = null;
        let activo = true;

        function tick() {
            if (!activo) return;
            if (document.hidden) return; // no gastamos backend en pestañas ocultas
            try {
                fn();
            } catch (e) {
                console.warn("Polling: error en callback", e);
            }
        }

        timer = window.setInterval(tick, intervaloMs || 12000);

        return function stop() {
            activo = false;
            if (timer) window.clearInterval(timer);
        };
    }

    window.DDPolling = { watch: watch };
})();
