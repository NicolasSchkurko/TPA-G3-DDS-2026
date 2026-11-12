/* =========================================================
   DDSI · Landing
   Demo de mapa de donaciones con Leaflet + datos de ejemplo.
   Si Leaflet no carga, se dibuja un mapa estático de respaldo.
   ========================================================= */
(function () {
    "use strict";

    /* ----------------------------- Datos de ejemplo ----------------------------- */
    const MAP_CENTER = [-34.6200, -58.4300]; // Centro geográfico de la red en CABA

    const DONATIONS = [
        {
            id: "piletones",
            title: "Alimentos no perecederos",
            org: "Comedor Los Piletones",
            area: "Villa Soldati",
            status: "delivered",
            icon: "basket2",
            coords: [-34.6538, -58.4478]
        },
        {
            id: "abrigo",
            title: "Ropa de abrigo",
            org: "Hogar San José",
            area: "Barracas",
            status: "transit",
            icon: "bag-heart",
            coords: [-34.6345, -58.3798]
        },
        {
            id: "escuela",
            title: "Útiles escolares",
            org: "Escuela N.º 12",
            area: "Balvanera",
            status: "delivered",
            icon: "book",
            coords: [-34.6095, -58.4085]
        },
        {
            id: "medicamentos",
            title: "Medicamentos e insumos",
            org: "Centro de Salud N.º 4",
            area: "Parque Patricios",
            status: "urgent",
            icon: "capsule",
            coords: [-34.6397, -58.4026]
        },
        {
            id: "juguetes",
            title: "Juguetes y libros",
            org: "Merendero Sonrisas",
            area: "Lugano",
            status: "transit",
            icon: "gift",
            coords: [-34.6807, -58.4752]
        }
    ];

    const STATUS = {
        delivered: { label: "Entregada", color: "#1b7838" },
        transit: { label: "En camino", color: "#6b7280" },
        urgent: { label: "Urgente", color: "#b45309" }
    };

    /* ----------------------------- Estado ----------------------------- */
    let map = null;
    let markersById = new Map();
    let activeId = null;

    /* ----------------------------- Utilidades ----------------------------- */
    function mapOnlyAndFeaturedDonations() {
        return Array.from(document.querySelectorAll(".map-donation-data")).flatMap((card) => {
            if (!card.dataset.latitude || !card.dataset.longitude) {
                return [];
            }

            const lat = Number(card.dataset.latitude);
            const lng = Number(card.dataset.longitude);
            if (!Number.isFinite(lat) || lat < -90 || lat > 90
                || !Number.isFinite(lng) || lng < -180 || lng > 180) {
                return [];
            }

            return [{
                id: card.dataset.id,
                title: card.querySelector(".card-title")?.textContent.trim() || "Donación",
                org: card.querySelector(".card-meta > span")?.textContent.trim() || "Entidad beneficiaria",
                area: card.dataset.area || "",
                status: "delivered",
                icon: "gift",
                coords: [lat, lng],
                url: card.getAttribute("href")
            }];
        });
    }

    function mapDonations() {
        return DONATIONS.concat(mapOnlyAndFeaturedDonations());
    }

    const byId = (id) => mapDonations().find((d) => d.id === id);

    function statusColor(status) {
        return (STATUS[status] || STATUS.delivered).color;
    }

    function statusLabel(status) {
        return (STATUS[status] || STATUS.delivered).label;
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    // Reutiliza los <symbol> del sprite SVG del template.
    function iconSvg(name) {
        return `<svg class="icon" aria-hidden="true"><use href="#i-${name}"></use></svg>`;
    }

    function prefersReducedMotion() {
        return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    }

    // Un marker de Leaflet expone getElement(); el fallback guarda el nodo en .element.
    function markerElement(marker) {
        if (!marker) return null;
        return typeof marker.getElement === "function" ? marker.getElement() : marker.element || null;
    }

    /* ----------------------------- Lista lateral ----------------------------- */
    function renderList() {
        const list = document.getElementById("donationList");
        if (!list) return;

        const count = document.getElementById("panelCount");
        if (count) {
            count.textContent = String(list.querySelectorAll(".donation-card").length);
        }
    }

    function setActive(id) {
        activeId = id;

        document.querySelectorAll(".donation-card").forEach((card) => {
            card.classList.toggle("is-active", card.dataset.id === id);
        });

        markersById.forEach((marker, markerId) => {
            const el = markerElement(marker);
            if (el) el.classList.toggle("is-active", markerId === id);
        });
    }

    function bindCardMarkerInteractions() {
        document.querySelectorAll(".donation-card").forEach((card) => {
            const marker = markersById.get(card.dataset.id);
            const element = markerElement(marker);
            if (!element) return;

            card.addEventListener("mouseenter", () => element.classList.add("is-active"));
            card.addEventListener("mouseleave", () => {
                if (activeId !== card.dataset.id) {
                    element.classList.remove("is-active");
                }
            });
        });
    }

    function focusDonation(id) {
        const donation = byId(id);
        if (!donation) return;

        setActive(id);

        if (!map) {
            const el = markerElement(markersById.get(id));
            if (el) el.classList.add("is-active");
            return;
        }

        const isMobile = window.matchMedia("(max-width: 860px)").matches;
        const zoom = Math.max(map.getZoom(), 14);

        if (isMobile) {
            const mapPane = document.querySelector(".map-pane");
            if (mapPane) {
                mapPane.scrollIntoView({
                    behavior: prefersReducedMotion() ? "auto" : "smooth",
                    block: "center"
                });
            }
        }

        map.flyTo(donation.coords, zoom, { duration: prefersReducedMotion() ? 0 : 0.7 });

        const marker = markersById.get(id);
        if (marker) {
            window.setTimeout(() => marker.openPopup(), prefersReducedMotion() ? 0 : 600);
        }
    }

    /* ----------------------------- Mapa con Leaflet ----------------------------- */
    function initMap() {
        map = window.DDSIMap.create({
            container: "map",
            center: MAP_CENTER,
            zoom: 13,
            minZoom: 11,
            maxZoom: 18,
            scrollWheelZoom: false,
            zoomControl: true,
            zoomControlPosition: "bottomright"
        });

        if (!map) {
            initFallbackMap();
            return;
        }

        // Las capas propias del landing van encima del mapa base.
        // Marcadores de donaciones.
        mapDonations().forEach((donation) => {
            const icon = L.divIcon({
                className: `donation-marker status-${donation.status}`,
                html: `
                    <span class="marker-wrap" style="--marker-color:${statusColor(donation.status)}">
                        <span class="marker-dot">${iconSvg(donation.icon)}</span>
                    </span>
                `,
                iconSize: [40, 40],
                iconAnchor: [20, 20],
                popupAnchor: [0, -18]
            });

            const marker = L.marker(donation.coords, {
                icon,
                riseOnHover: true,
                title: donation.title
            }).addTo(map);

            marker.bindPopup(popupHtml(donation), {
                closeButton: false,
                offset: [0, -4],
                minWidth: 210
            });

            marker.on("click", () => setActive(donation.id));
            marker.on("mouseover", () => {
                const card = document.querySelector(`.donation-card[data-id="${donation.id}"]`);
                if (card) card.classList.add("is-active");
            });
            marker.on("mouseout", () => {
                if (activeId === donation.id) return;
                const card = document.querySelector(`.donation-card[data-id="${donation.id}"]`);
                if (card) card.classList.remove("is-active");
            });

            markersById.set(donation.id, marker);
        });
        bindCardMarkerInteractions();

        // Encuadre inicial. El ResizeObserver espera a que el contenedor tenga
        // tamaño real y también reencuadra al cambiar el tamaño de la ventana.
        window.requestAnimationFrame(() => {
            map.invalidateSize();
            fitMap();
        });
        window.addEventListener("load", fitMap);
        window.setTimeout(fitMap, 400);

        if (typeof ResizeObserver !== "undefined") {
            const mapElement = document.getElementById("map");
            const observer = new ResizeObserver(() => {
                map.invalidateSize();
                fitMap();
            });
            observer.observe(mapElement);
        }
    }

    // Encuadra los puntos en el espacio que deja libre el panel.
    function fitMap() {
        if (!map) return;

        // Si el contenedor todavía no tiene tamaño, esperamos al próximo frame.
        const size = map.getSize();
        if (!size.x || !size.y) {
            window.requestAnimationFrame(fitMap);
            return;
        }

        const isMobile = window.matchMedia("(max-width: 860px)").matches;
        const bounds = L.latLngBounds(mapDonations().map((d) => d.coords));
        const padding = isMobile ? 48 : 72;

        map.fitBounds(bounds, {
            padding: [padding, padding],
            animate: false
        });
    }

    function popupHtml(donation) {
        return `
            <img class="popup-image" src="/images/donaciones/ejemplo.png"
                 alt="Imagen de ejemplo de una donación">
            <span class="popup-eyebrow" style="color:${statusColor(donation.status)}">
                <i class="legend-dot ${donation.status}" aria-hidden="true"></i>
                ${statusLabel(donation.status)}
            </span>
            <div class="popup-title">${escapeHtml(donation.title)}</div>
            <div class="popup-meta">${escapeHtml(donation.org)} · ${escapeHtml(donation.area)}</div>
            <a class="popup-link" href="${escapeHtml(donation.url || "/donaciones")}">Ver donación ${iconSvg("arrow-right")}</a>
        `;
    }

    /* ----------------------------- Fallback sin Leaflet ----------------------------- */
    function initFallbackMap() {
        const container = document.getElementById("map");
        if (!container) return;

        container.classList.add("map-fallback");

        const svgNs = "http://www.w3.org/2000/svg";
        const svg = document.createElementNS(svgNs, "svg");
        svg.setAttribute("class", "fallback-streets");
        svg.setAttribute("viewBox", "0 0 1200 800");
        svg.setAttribute("preserveAspectRatio", "xMidYMid slice");

        let paths = "";
        for (let i = 0; i < 15; i += 1) {
            const y = 30 + i * 56;
            const bend = (i % 2 === 0 ? 1 : -1) * (40 + ((i * 13) % 60));
            const avenue = i % 4 === 0 ? ' class="avenue"' : "";
            paths += `<path d="M-60 ${y} C 260 ${y + bend}, 620 ${y - bend}, 1260 ${y}"${avenue}></path>`;
        }
        for (let i = 0; i < 16; i += 1) {
            const x = 30 + i * 80;
            const bend = (i % 2 === 0 ? -1 : 1) * (30 + ((i * 17) % 50));
            const avenue = i % 5 === 0 ? ' class="avenue"' : "";
            paths += `<path d="M${x} -60 C ${x + bend} 240, ${x - bend} 560, ${x} 860"${avenue}></path>`;
        }
        svg.innerHTML = paths;
        container.appendChild(svg);

        // Donaciones ubicadas de forma proporcional al área metropolitana.
        const donations = mapDonations();
        const lats = donations.map((d) => d.coords[0]);
        const lngs = donations.map((d) => d.coords[1]);
        const minLat = Math.min(...lats) - 0.015;
        const maxLat = Math.max(...lats) + 0.015;
        const minLng = Math.min(...lngs) - 0.015;
        const maxLng = Math.max(...lngs) + 0.015;

        function position(coords) {
            const left = 10 + ((coords[1] - minLng) / (maxLng - minLng)) * 80;
            const top = 12 + ((maxLat - coords[0]) / (maxLat - minLat)) * 76;
            return { left, top };
        }

        donations.forEach((donation) => {
            const pos = position(donation.coords);
            const button = document.createElement("button");
            button.type = "button";
            button.className = "fallback-marker";
            button.style.left = `${pos.left}%`;
            button.style.top = `${pos.top}%`;
            button.title = `${donation.title} · ${donation.org}`;
            button.innerHTML = `
                <span class="marker-wrap" style="--marker-color:${statusColor(donation.status)}">
                    <span class="marker-dot">${iconSvg(donation.icon)}</span>
                </span>
            `;
            button.addEventListener("click", () => {
                setActive(donation.id);
                button.classList.add("is-active");
            });
            container.appendChild(button);

            markersById.set(donation.id, { element: button, openPopup: () => {} });
        });

        // En el fallback no hay zoom: al tocar una tarjeta solo se resalta el punto.
        bindCardMarkerInteractions();
    }

    /* ----------------------------- Arranque ----------------------------- */
    function init() {
        renderList();

        if (window.DDSIMap && window.DDSIMap.isAvailable()) {
            initMap();
        } else {
            initFallbackMap();
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
