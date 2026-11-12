/* =========================================================
   DDSI · Seguimiento en vivo
   Mapa + lista de camiones + "última actualización" relativa.
   ========================================================= */
(function () {
    "use strict";

    const refreshBtn = document.getElementById("refreshSeguimiento");
    const lastRefresh = document.getElementById("lastRefresh");
    const lastUpdateNodes = document.querySelectorAll(".last-update");

    const data = window.ENTIDAD_SEGUIMIENTO || {};
    const pollingMs = window.ENTIDAD_POLLING_MS || 30000;
    const ctx = window.EntidadMap ? window.EntidadMap.create("map", data) : null;

    let ultimaActualizacion = Date.now();

    function textoActualizado() {
        const segundos = Math.max(0, Math.round((Date.now() - ultimaActualizacion) / 1000));
        if (segundos < 5) return "Actualizado recién";
        if (segundos < 60) return "Actualizado hace " + segundos + " s";
        return "Actualizado hace " + Math.floor(segundos / 60) + " min";
    }

    function tickUltimaActualizacion() {
        if (lastRefresh) lastRefresh.textContent = textoActualizado();
        lastUpdateNodes.forEach(function (node) {
            const desde = node.dataset.since;
            if (!desde) return;
            const ts = Date.parse(desde);
            if (Number.isNaN(ts)) return;
            const segundos = Math.max(0, Math.round((Date.now() - ts) / 1000));
            node.textContent = "hace " + (segundos < 60 ? segundos + " s" : Math.floor(segundos / 60) + " min");
        });
    }

    async function refrescar() {
        try {
            const response = await fetch("/api/entidad/seguimiento", { headers: { "Accept": "application/json" } });
            if (!response.ok) throw new Error("HTTP " + response.status);
            const nuevo = await response.json();
            if (ctx && window.EntidadMap) window.EntidadMap.refresh(ctx, nuevo);
            ultimaActualizacion = Date.now();
            tickUltimaActualizacion();
        } catch (e) {
            console.warn("No se pudo actualizar el seguimiento:", e);
            if (window.DDToast) window.DDToast.show("error", "No pudimos actualizar la ubicación. Reintentá en unos segundos.");
        }
    }

    if (refreshBtn) refreshBtn.addEventListener("click", refrescar);

    window.setInterval(tickUltimaActualizacion, 1000);
    tickUltimaActualizacion();

    if (window.DDPolling && ctx) {
        window.DDPolling.watch(refrescar, pollingMs);
    }
})();
