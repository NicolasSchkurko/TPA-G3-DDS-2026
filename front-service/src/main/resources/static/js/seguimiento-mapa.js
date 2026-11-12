/* =========================================================
   DonaTrack · Mapa público de seguimiento
   ========================================================= */
(function () {
    "use strict";

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function iniciar() {
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

        const layer = window.DDSIMap.addGeoJson(map, geojson, {
            style: function () {
                return { color: "#1b7838", weight: 4, opacity: 0.88 };
            },
            pointToLayer: function (feature, latlng) {
                const propiedades = feature.properties || {};
                const estado = propiedades.estado;
                const clase = propiedades.deposito ? " is-deposito"
                    : (estado === "actual" ? " is-actual" : (estado === "completa" ? " is-completa" : ""));
                const numero = propiedades.deposito ? "D" : (propiedades.parada || "");
                const icon = L.divIcon({
                    className: "",
                    html: '<span class="ruta-pin' + clase + '">' + escapeHtml(numero) + "</span>",
                    iconSize: [30, 30],
                    iconAnchor: [15, 15],
                    popupAnchor: [0, -16]
                });
                return L.marker(latlng, { icon: icon, keyboard: true });
            },
            onEachFeature: function (feature, capa) {
                if (!feature.geometry || feature.geometry.type !== "Point") return;
                const p = feature.properties || {};
                if (p.deposito) {
                    capa.bindPopup("<strong>" + escapeHtml(p.etiqueta || "Depósito") + "</strong><br>Punto de partida de la ruta");
                    return;
                }
                capa.bindPopup(
                    "<strong>Parada " + escapeHtml(p.parada) + "</strong><br>" +
                    escapeHtml(p.direccion) + "<br>" +
                    (p.entregadas || 0) + "/" + (p.total || 0) + " entregadas"
                );
            }
        });

        window.DDSIMap.fit(map, layer, [40, 40]);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", iniciar);
    } else {
        iniciar();
    }
})();
