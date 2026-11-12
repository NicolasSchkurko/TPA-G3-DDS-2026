/* =========================================================
   DDSI · Mapa Leaflet compartido del portal de entidad
   Marcador de entidad + camiones con popup y polling.
   Si Leaflet no carga, muestra un mensaje en lugar de romperse.
   ========================================================= */
(function () {
    "use strict";

    const HOME_SVG = '<svg viewBox="0 0 16 16" fill="currentColor" aria-hidden="true"><path d="M8 1 1.5 7v7a1 1 0 0 0 1 1h4v-4h3v4h4a1 1 0 0 0 1-1V7L8 1z"/></svg>';
    const TRUCK_SVG = '<svg viewBox="0 0 16 16" fill="currentColor" aria-hidden="true"><path d="M0 3.5A1.5 1.5 0 0 1 1.5 2h9A1.5 1.5 0 0 1 12 3.5V5h1.02a1.5 1.5 0 0 1 1.17.56l1.48 1.85a1.5 1.5 0 0 1 .33.94v3.15a1.5 1.5 0 0 1-1.5 1.5H14a2 2 0 1 1-4 0H6a2 2 0 1 1-4 0H1.5A1.5 1.5 0 0 1 0 10.5z"/></svg>';

    function divIcon(kind) {
        return L.divIcon({
            className: "",
            html: '<div class="map-marker map-marker-' + kind + (kind === "home" ? " marker-pulse" : "") + '">' + (kind === "home" ? HOME_SVG : TRUCK_SVG) + "</div>",
            iconSize: [34, 34],
            iconAnchor: [17, 17],
            popupAnchor: [0, -20]
        });
    }

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;").replace(/'/g, "&#039;");
    }

    function popupCamion(camion) {
        const donaciones = Array.isArray(camion.donaciones) ? camion.donaciones.length : 0;
        const link = donaciones > 0 && camion.donaciones[0]
            ? '<br><a href="/entidad/donaciones/' + encodeURIComponent(camion.donaciones[0]) + '">Ver donación →</a>'
            : "";
        return "<strong>" + escapeHtml(camion.patente) + "</strong><br>"
            + "Ruta: " + escapeHtml(camion.estadoRuta || "en curso") + "<br>"
            + (camion.destino ? "En camino a: " + escapeHtml(camion.destino) + "<br>" : "")
            + "ETA aprox.: " + (camion.etaMinutos != null ? camion.etaMinutos + " min" : "sin datos") + "<br>"
            + donaciones + " entrega(s)" + link;
    }

    /** Crea el mapa con los datos actuales. Devuelve null si Leaflet no está disponible. */
    function create(containerId, data) {
        const container = document.getElementById(containerId);
        if (typeof L === "undefined") {
            if (container) {
                container.innerHTML = '<div class="map-fallback" style="display:grid;place-items:center;height:100%;color:var(--muted);font-size:14px;padding:24px;text-align:center;">'
                    + "No pudimos cargar el mapa. Revisá tu conexión y recargá la página.</div>";
            }
            return null;
        }

        const entidad = data && data.entidad ? data.entidad : { lat: -34.62, lng: -58.43, razonSocial: "Mi entidad", direccion: "" };
        const center = [Number(entidad.lat) || -34.62, Number(entidad.lng) || -58.43];
        const map = L.map(containerId, { scrollWheelZoom: false, attributionControl: true }).setView(center, 12);

        L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
            maxZoom: 19,
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(map);

        const homeMarker = L.marker(center, { icon: divIcon("home"), title: entidad.razonSocial || "Mi entidad" })
            .addTo(map)
            .bindPopup("<strong>" + escapeHtml(entidad.razonSocial || "Mi entidad") + "</strong><br>" + escapeHtml(entidad.direccion || ""));

        const ctx = { map: map, homeMarker: homeMarker, markers: new Map(), lines: new Map() };
        const depositoLat = data && data.depositoLat != null ? Number(data.depositoLat) : NaN;
        const depositoLng = data && data.depositoLng != null ? Number(data.depositoLng) : NaN;
        if (Number.isFinite(depositoLat) && Number.isFinite(depositoLng)) {
            const deposito = L.circleMarker([depositoLat, depositoLng], {
                radius: 11,
                color: "#0f1210",
                weight: 3,
                fillColor: "#1b7838",
                fillOpacity: 1
            }).addTo(map).bindPopup("<strong>" + escapeHtml(data.depositoEtiqueta || "Depósito") + "</strong><br>Punto de partida de las rutas");
            ctx.deposito = deposito;
        }
        refresh(ctx, data);

        const puntos = [center.slice()];
        ctx.markers.forEach(function (marker) { puntos.push(marker.getLatLng()); });
        if (ctx.deposito) puntos.push(ctx.deposito.getLatLng());
        if (puntos.length > 1) {
            map.fitBounds(L.latLngBounds(puntos).pad(0.35));
        }

        return ctx;
    }

    /** Actualiza marcadores de camiones con los datos del último polling. */
    function refresh(ctx, data) {
        if (!ctx || !data) return;
        const camiones = Array.isArray(data.camiones) ? data.camiones : [];
        const vistos = new Set();

        camiones.forEach(function (camion) {
            if (!camion || !camion.patente) return;
            vistos.add(camion.patente);
            const lat = Number(camion.lat), lng = Number(camion.lng);
            if (Number.isNaN(lat) || Number.isNaN(lng)) return;

            if (ctx.markers.has(camion.patente)) {
                const marker = ctx.markers.get(camion.patente);
                marker.setLatLng([lat, lng]);
                marker.setPopupContent(popupCamion(camion));
            } else {
                const marker = L.marker([lat, lng], { icon: divIcon("truck"), title: "Camión " + camion.patente })
                    .addTo(ctx.map)
                    .bindPopup(popupCamion(camion));
                ctx.markers.set(camion.patente, marker);
            }

            const trazado = Array.isArray(camion.trazado) ? camion.trazado : [];
            if (trazado.length >= 2) {
                const puntosRuta = trazado.map(function (p) { return [p[1], p[0]]; });
                if (ctx.lines.has(camion.patente)) {
                    ctx.lines.get(camion.patente).setLatLngs(puntosRuta);
                } else {
                    ctx.lines.set(camion.patente, L.polyline(puntosRuta, {
                        color: "#1b7838",
                        weight: 3,
                        opacity: 0.5,
                        dashArray: "6 6"
                    }).addTo(ctx.map));
                }
            }
        });

        ctx.markers.forEach(function (marker, patente) {
            if (!vistos.has(patente)) {
                ctx.map.removeLayer(marker);
                ctx.markers.delete(patente);
            }
        });

        if (ctx.lines) {
            ctx.lines.forEach(function (line, patente) {
                if (!vistos.has(patente)) {
                    ctx.map.removeLayer(line);
                    ctx.lines.delete(patente);
                }
            });
        }
    }

    window.EntidadMap = { create: create, refresh: refresh };
})();
