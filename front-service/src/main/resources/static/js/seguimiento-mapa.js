/* =========================================================
   DonaTrack · Mapa completo de una ruta
   Paradas, depósito y ubicación del camión sobre el recorrido.
   ========================================================= */
(function () {
    "use strict";

    const MARKERS = new Map();
    let MAPA = null;

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function prefiereMenosMovimiento() {
        return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    }

    function iniciarMapa() {
        const container = document.getElementById("map");
        const geojson = window.SEGUIMIENTO_GEOJSON;
        if (!container || !geojson || !window.DDSIMap || !window.DDSIMap.isAvailable()) return;

        const map = window.DDSIMap.create({
            container: "map",
            center: [-34.6037, -58.3816],
            zoom: 12,
            scrollWheelZoom: false
        });
        if (!map) return;
        MAPA = map;

        const layer = window.DDSIMap.addGeoJson(map, geojson, {
            style: function () {
                return { color: "#1b7838", weight: 4, opacity: 0.88 };
            },
            pointToLayer: function (feature, latlng) {
                const p = feature.properties || {};
                if (p.camion) {
                    const truckIcon = L.divIcon({
                        className: "",
                        html: '<span class="ruta-pin is-camion" aria-hidden="true">&#128666;</span>',
                        iconSize: [40, 40],
                        iconAnchor: [20, 20],
                        popupAnchor: [0, -22]
                    });
                    return L.marker(latlng, {
                        icon: truckIcon,
                        keyboard: true,
                        zIndexOffset: 800
                    });
                }

                const estado = p.estado;
                const clase = p.deposito ? " is-deposito"
                    : (estado === "actual" ? " is-actual" : (estado === "completa" ? " is-completa" : ""));
                const numero = p.deposito ? "D" : (p.parada || "");
                const icon = L.divIcon({
                    className: "",
                    html: '<span class="ruta-pin' + clase + '">' + escapeHtml(numero) + "</span>",
                    iconSize: [30, 30],
                    iconAnchor: [15, 15],
                    popupAnchor: [0, -16]
                });
                const marker = L.marker(latlng, { icon: icon, keyboard: true });
                const nro = p.deposito ? 0 : Number(numero);
                if (Number.isFinite(nro)) MARKERS.set(nro, marker);
                return marker;
            },
            onEachFeature: function (feature, capa) {
                if (!feature.geometry || feature.geometry.type !== "Point") return;
                const p = feature.properties || {};
                if (p.camion) {
                    capa.bindPopup("<strong>Camión " + escapeHtml(p.etiqueta || "") + "</strong><br>Ubicación actual en la ruta");
                    return;
                }
                if (p.deposito) {
                    capa.bindPopup("<strong>" + escapeHtml(p.etiqueta || "Depósito") + "</strong><br>Punto de partida de la ruta");
                    return;
                }
                capa.bindPopup(
                    "<strong>Parada " + escapeHtml(p.parada) + "</strong><br>" +
                    escapeHtml(p.direccion) + "<br>" +
                    (p.entregadas || 0) + "/" + (p.total || 0) + " entregados"
                );
            }
        });

        const desktop = window.matchMedia("(min-width: 901px)").matches;
        window.DDSIMap.fit(map, layer, desktop ? [40, 380] : [24, 24]);
    }

    function enfocarParada(numero) {
        if (!MAPA) return;
        const marker = MARKERS.get(Number(numero));
        if (!marker) return;
        MAPA.flyTo(marker.getLatLng(), Math.max(MAPA.getZoom(), 16), {
            duration: prefiereMenosMovimiento() ? 0 : 0.6
        });
        window.setTimeout(function () {
            marker.openPopup();
        }, prefiereMenosMovimiento() ? 0 : 650);
    }

    function iniciarDrawer() {
        const drawer = document.getElementById("seguimientoDrawer");
        const toggle = document.getElementById("toggleDrawer");
        const fab = document.getElementById("toggleDrawerFab");
        if (!drawer) return;

        function actualizar(abierto) {
            drawer.classList.toggle("is-collapsed", !abierto);
            const shell = drawer.closest(".seguimiento-map-shell");
            if (shell) shell.classList.toggle("drawer-collapsed", !abierto);
            if (toggle) {
                toggle.setAttribute("aria-expanded", String(abierto));
                const label = toggle.querySelector(".visually-hidden");
                if (label) label.textContent = abierto ? "Contraer panel de paradas" : "Expandir panel de paradas";
            }
            if (fab) fab.setAttribute("aria-expanded", String(abierto));
        }

        function alternar() {
            actualizar(drawer.classList.contains("is-collapsed"));
        }

        if (toggle) toggle.addEventListener("click", alternar);
        if (fab) fab.addEventListener("click", alternar);

        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape" && window.matchMedia("(max-width: 900px)").matches) {
                actualizar(false);
            }
        });
    }

    function iniciarParadas() {
        document.querySelectorAll("[data-map-parada]").forEach(function (control) {
            control.addEventListener("click", function () {
                enfocarParada(control.dataset.mapParada);
                if (window.matchMedia("(max-width: 900px)").matches) {
                    const toggle = document.getElementById("toggleDrawer");
                    if (toggle) toggle.click();
                }
            });
        });
    }

    function iniciar() {
        iniciarMapa();
        iniciarDrawer();
        iniciarParadas();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", iniciar);
    } else {
        iniciar();
    }
})();
