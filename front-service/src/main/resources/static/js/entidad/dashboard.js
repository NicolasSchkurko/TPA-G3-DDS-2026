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
    const donaciones = Array.isArray(window.ENTIDAD_DONACIONES) ? window.ENTIDAD_DONACIONES : [];
    const donationMarkers = new Map();
    const mapSelectionNotice = document.getElementById("mapSelectionNotice");

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;").replace(/'/g, "&#039;");
    }

    const DONATION_STATUS_COLORS = {
        ENTREGADO: "#1b7838",
        ENTREGADA: "#1b7838",
        EN_TRASLADO: "#6b7280",
        EN_CAMINO: "#6b7280",
        ASIGNADO: "#b45309",
        LISTA_PARA_ENTREGAR: "#b45309",
        PENDIENTE: "#b45309",
        PENDIENTE_ASIGNACION: "#b45309",
        ENTREGA_FALLIDA: "#b42318",
        VENCIDO: "#b42318"
    };

    function colorEstado(status) {
        return DONATION_STATUS_COLORS[String(status || "").toUpperCase()] || "#5b635c";
    }

    function etiquetaEstado(donacion, entrega) {
        if (entrega && entrega.estado === "EN_CAMINO") return "En camino";
        if (entrega && entrega.estado === "PENDIENTE") return "Pendiente";
        return donacion && donacion.estadoEtiqueta ? donacion.estadoEtiqueta : "Sin estado";
    }

    function popupDonacion(donacion, entrega) {
        const titulo = donacion && donacion.titulo ? donacion.titulo : "Donación";
        const estado = etiquetaEstado(donacion, entrega);
        const entidad = data.entidad && data.entidad.razonSocial ? data.entidad.razonSocial : "Mi entidad";
        const zona = data.entidad && data.entidad.zona ? data.entidad.zona : "";
        const href = donacion && donacion.id
            ? `/entidad/donaciones/${encodeURIComponent(donacion.id)}`
            : "/entidad/donaciones";
        return `<img class="popup-image" src="/images/donaciones/ejemplo.png"
                     alt="Imagen de ejemplo de una donación">`
            + `<span class="popup-eyebrow" style="color:${colorEstado(entrega && entrega.estado || donacion && donacion.estado)}">`
            + `${escapeHtml(estado)}</span>`
            + `<div class="popup-title">${escapeHtml(titulo)}</div>`
            + `<div class="popup-meta">${escapeHtml(entidad)}`
            + (zona ? ` · ${escapeHtml(zona)}` : "") + `</div>`
            + `<a class="popup-link" href="${href}">Ver donación →</a>`;
    }

    function focusDonacion(id) {
        if (!ctx || !ctx.map || !id) return false;
        const key = String(id);
        const donacion = donaciones.find((d) => String(d.id) === key);
        const entrega = (data.entregas || []).find((e) => String(e.idDonacion) === key);
        const camion = (data.camiones || []).find((c) =>
            Array.isArray(c.donaciones) && c.donaciones.some((d) => String(d) === key));

        let marker = camion ? ctx.markers.get(camion.patente) : null;
        const lat = entrega && entrega.lat != null ? Number(entrega.lat) : NaN;
        const lng = entrega && entrega.lng != null ? Number(entrega.lng) : NaN;
        if (!marker && Number.isFinite(lat) && Number.isFinite(lng)
                && Math.abs(lat) <= 90 && Math.abs(lng) <= 180) {
            marker = donationMarkers.get(key);
            if (!marker) {
                marker = L.circleMarker([lat, lng], {
                    radius: 10,
                    color: "#155f2d",
                    weight: 2,
                    fillColor: "#1b7838",
                    fillOpacity: 0.9
                }).addTo(ctx.map);
                donationMarkers.set(key, marker);
            }
        }
        if (!marker) marker = ctx.homeMarker;
        if (!marker) {
            if (mapSelectionNotice) {
                mapSelectionNotice.textContent = "No hay una ubicación disponible para esta donación.";
            }
            return false;
        }

        marker.setPopupContent(popupDonacion(donacion, entrega));
        if (mapSelectionNotice) {
            mapSelectionNotice.textContent = `Donación: ${donacion && donacion.titulo ? donacion.titulo : id}.`;
        }
        ctx.map.flyTo(marker.getLatLng(), Math.max(ctx.map.getZoom(), 14), { duration: 0.6 });
        window.setTimeout(() => marker.openPopup(), 650);
        return true;
    }

    document.querySelectorAll("[data-donacion]").forEach((link) => {
        link.addEventListener("click", (event) => {
            if (!ctx || !ctx.map || !link.dataset.donacion) return;
            event.preventDefault();
            focusDonacion(link.dataset.donacion);
        });
    });

    const donacionParam = new URLSearchParams(window.location.search).get("donacion");
    if (donacionParam) {
        window.setTimeout(() => focusDonacion(donacionParam), 300);
    }

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
