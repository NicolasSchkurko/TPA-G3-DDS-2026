/* =========================================================
   DDSI · Dashboard de entidad
   Mapa de fondo + drawers de necesidades y datos de entidad.
   ========================================================= */
(function () {
    "use strict";

    const main = document.getElementById("contenido");
    const leftPanel = document.getElementById("necesidadesPanel");
    const leftToggle = document.getElementById("toggleNecesidades");
    const rightPanel = document.getElementById("entidadDataPanel");
    const rightToggle = document.getElementById("toggleEntidadData");
    const mapError = document.getElementById("mapError");
    const mapRetry = document.getElementById("mapRetry");

    /* ---------- Drawer izquierdo (necesidades) ---------- */
    function setLeftCollapsed(collapsed) {
        if (!main) return;
        main.classList.toggle("sidebar-collapsed", collapsed);
        if (leftToggle) {
            leftToggle.setAttribute("aria-expanded", String(!collapsed));
            leftToggle.setAttribute("aria-label", collapsed ? "Mostrar necesidades de la entidad" : "Ocultar necesidades de la entidad");
            const icon = leftToggle.querySelector("i");
            if (icon) icon.className = collapsed ? "bi bi-chevron-right" : "bi bi-chevron-left";
            const label = leftToggle.querySelector(".map-corner-toggle-label");
            if (label) label.textContent = collapsed ? "Necesidades" : "Ocultar";
        }
    }

    if (leftToggle) {
        leftToggle.addEventListener("click", function () {
            setLeftCollapsed(!main.classList.contains("sidebar-collapsed"));
            if (ctx && window.EntidadMap) window.setTimeout(function () { /* noop */ }, 0);
            invalidateMap();
        });
    }

    /* ---------- Drawer derecho (datos de entidad) ---------- */
    function setRightOpen(open) {
        if (!rightPanel || !rightToggle) return;
        rightToggle.setAttribute("aria-expanded", String(open));
        rightPanel.setAttribute("aria-hidden", String(!open));
        rightPanel.classList.toggle("is-open", open);
        const mapSection = rightToggle.closest(".beneficiary-map");
        if (mapSection) mapSection.classList.toggle("options-open", open);
        rightToggle.setAttribute("aria-label", open ? "Cerrar datos de la entidad" : "Abrir datos de la entidad");
        const icon = rightToggle.querySelector("i");
        if (icon) icon.className = open ? "bi bi-chevron-right" : "bi bi-chevron-left";
        const label = rightToggle.querySelector(".map-corner-toggle-label");
        if (label) label.textContent = open ? "Cerrar" : "Datos";
        if (open) {
            const first = rightPanel.querySelector("a, button, input, [tabindex]:not([tabindex='-1'])");
            if (first) first.focus();
        } else {
            rightToggle.focus();
        }
        invalidateMap();
    }

    if (rightToggle) {
        rightToggle.addEventListener("click", function () {
            setRightOpen(rightToggle.getAttribute("aria-expanded") !== "true");
        });
    }

    function invalidateMap() {
        if (ctx && ctx.map && typeof ctx.map.invalidateSize === "function") {
            window.setTimeout(function () { ctx.map.invalidateSize(); }, 220);
        }
    }

    document.addEventListener("keydown", function (event) {
        if (event.key !== "Escape") return;
        if (rightPanel && rightPanel.classList.contains("is-open")) {
            setRightOpen(false);
        }
    });

    /* Estado inicial */
    setLeftCollapsed(true);

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
