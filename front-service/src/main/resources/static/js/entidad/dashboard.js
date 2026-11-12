/* =========================================================
   DDSI · Dashboard de entidad
   Panel desplegable + seguimiento en el mapa con polling.
   ========================================================= */
(function () {
    "use strict";

    const main = document.getElementById("contenido");
    const panel = document.getElementById("panelEntidad");
    const collapseBtn = document.getElementById("panelCollapse");
    const expandBtn = document.getElementById("panelExpand");
    const handle = document.getElementById("panelHandle");
    const mapError = document.getElementById("mapError");
    const mapRetry = document.getElementById("mapRetry");

    /* ---------- Panel desplegable ---------- */
    const mqDesktop = window.matchMedia("(min-width: 861px)");
    const STORAGE_KEY = "entidad.panel.collapsed";

    function setMobileOpen(open) {
        if (!panel) return;
        panel.classList.toggle("is-open", open);
        if (handle) handle.setAttribute("aria-expanded", String(open));
    }

    function aplicarColapsado(colapsado) {
        if (!main) return;
        main.classList.toggle("is-collapsed", colapsado);
        if (collapseBtn) {
            collapseBtn.setAttribute("aria-expanded", String(!colapsado));
            collapseBtn.setAttribute("aria-label", colapsado ? "Expandir panel" : "Contraer panel");
        }
        if (expandBtn) expandBtn.setAttribute("aria-expanded", String(!colapsado));
        if (handle) handle.setAttribute("aria-expanded", String(!colapsado));
        try {
            localStorage.setItem(STORAGE_KEY, colapsado ? "1" : "0");
        } catch (e) {
            // localStorage puede estar bloqueado; el panel funciona igual.
        }
    }

    function expandir() {
        aplicarColapsado(false);
        setMobileOpen(true);
        panel && panel.focus && panel.focus();
    }

    function toggle() {
        if (mqDesktop.matches) {
            aplicarColapsado(!main.classList.contains("is-collapsed"));
        } else {
            setMobileOpen(!panel.classList.contains("is-open"));
        }
    }

    if (collapseBtn) collapseBtn.addEventListener("click", toggle);
    if (expandBtn) expandBtn.addEventListener("click", expandir);
    if (handle) handle.addEventListener("click", toggle);

    // En desktop el panel arranca expandido salvo que el usuario lo haya contraído antes;
    // en mobile arranca como hoja inferior (peek).
    if (mqDesktop.matches) {
        let colapsado = false;
        try {
            colapsado = localStorage.getItem(STORAGE_KEY) === "1";
        } catch (e) {
            colapsado = false;
        }
        aplicarColapsado(colapsado);
    } else {
        setMobileOpen(false);
    }

    /* ---------- Mapa + polling ---------- */
    const data = window.ENTIDAD_SEGUIMIENTO || {};
    const pollingMs = window.ENTIDAD_POLLING_MS || 30000;
    const ctx = window.EntidadMap ? window.EntidadMap.create("map", data) : null;

    async function refrescar() {
        try {
            const response = await fetch("/api/entidad/seguimiento", { headers: { "Accept": "application/json" } });
            if (!response.ok) throw new Error("HTTP " + response.status);
            const nuevo = await response.json();
            if (ctx && window.EntidadMap) window.EntidadMap.refresh(ctx, nuevo);
            if (mapError) mapError.hidden = true;
        } catch (e) {
            console.warn("No se pudo actualizar el seguimiento:", e);
            if (mapError) mapError.hidden = false;
        }
    }

    if (mapRetry) mapRetry.addEventListener("click", refrescar);

    if (window.DDPolling && ctx) {
        window.DDPolling.watch(refrescar, pollingMs);
    }
})();
