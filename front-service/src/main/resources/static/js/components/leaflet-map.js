/* =========================================================
   Componente de mapa reutilizable (Leaflet + OpenStreetMap).
   Pensado para cliente liviano: sin framework y con las capas
   que cada página necesite agregar encima.
   ========================================================= */
(function () {
    "use strict";

    const DEFAULTS = {
        container: "map",
        center: [-34.6037, -58.3816], // CABA
        zoom: 13,
        minZoom: 10,
        maxZoom: 19,
        zoomControl: true,
        zoomControlPosition: "bottomright",
        scrollWheelZoom: false,
        zoomSnap: 0.25,
        zoomDelta: 0.5,
        preferCanvas: true,
        tilesUrl: "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    };

    function isAvailable() {
        return !!(window.L && typeof window.L.map === "function");
    }

    function resolveContainer(container) {
        return typeof container === "string" ? document.getElementById(container) : container;
    }

    /**
     * Crea el mapa base. Devuelve null si Leaflet no cargó o no existe el contenedor.
     * Options override DEFAULTS. Cada página agrega después sus marcadores, polylines
     * o capas GeoJSON.
     */
    function create(options) {
        if (!isAvailable()) return null;

        const opts = Object.assign({}, DEFAULTS, options || {});
        const element = resolveContainer(opts.container);
        if (!element) return null;

        const map = L.map(element, {
            center: opts.center,
            zoom: opts.zoom,
            minZoom: opts.minZoom,
            maxZoom: opts.maxZoom,
            zoomControl: false,
            scrollWheelZoom: opts.scrollWheelZoom,
            zoomSnap: opts.zoomSnap,
            zoomDelta: opts.zoomDelta,
            preferCanvas: opts.preferCanvas,
            attributionControl: true
        });

        L.tileLayer(opts.tilesUrl, {
            maxZoom: opts.maxZoom,
            attribution: opts.attribution,
            // Menos pedidos de tiles mientras el usuario mueve el mapa.
            updateWhenIdle: true,
            keepBuffer: 2,
            detectRetina: false
        }).addTo(map);

        if (opts.zoomControl !== false) {
            L.control.zoom({ position: opts.zoomControlPosition }).addTo(map);
        }

        return map;
    }

    /**
     * Agrega una capa GeoJSON. Sirve para rutas o para cualquier geometría
     * que el backend devuelva ya calculada.
     */
    function addGeoJson(map, geojson, options) {
        if (!map || !geojson) return null;

        const opts = options || {};
        const layer = L.geoJSON(geojson, {
            style: opts.style || function () {
                return { color: "#2563eb", weight: 5, opacity: 0.9 };
            },
            pointToLayer: opts.pointToLayer,
            onEachFeature: opts.onEachFeature
        });

        layer.addTo(map);
        return layer;
    }

    /** Encuadra una capa (o grupo de capas) dentro del viewport. */
    function fit(map, layer, padding) {
        if (!map || !layer || typeof layer.getBounds !== "function") return;

        const bounds = layer.getBounds();
        if (!bounds || !bounds.isValid()) return;

        map.fitBounds(bounds, {
            padding: padding || [24, 24],
            animate: false
        });
    }

    window.DDSIMap = {
        isAvailable: isAvailable,
        create: create,
        addGeoJson: addGeoJson,
        fit: fit
    };
})();
