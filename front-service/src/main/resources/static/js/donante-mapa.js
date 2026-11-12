(function () {
    "use strict";

    let map = null;
    const markersByEntityId = new Map();
    const markersByEntityName = new Map();
    const truckMarkers = new Map();
    const truckLines = new Map();
    let depotMarker = null;

    const TRUCK_SVG = '<svg viewBox="0 0 16 16" fill="currentColor" aria-hidden="true"><path d="M0 3.5A1.5 1.5 0 0 1 1.5 2h9A1.5 1.5 0 0 1 12 3.5V5h1.02a1.5 1.5 0 0 1 1.17.56l1.48 1.85a1.5 1.5 0 0 1 .33.94v3.15a1.5 1.5 0 0 1-1.5 1.5H14a2 2 0 1 1-4 0H6a2 2 0 1 1-4 0H1.5A1.5 1.5 0 0 1 0 10.5z"/></svg>';

    function initMap() {
        if (!window.DDSIMap || !window.DDSIMap.isAvailable()) return;

        map = window.DDSIMap.create({
            container: "beneficiaryMap",
            center: [-34.6200, -58.4300],
            zoom: 13,
            minZoom: 5,
            maxZoom: 18,
            scrollWheelZoom: false,
            zoomControlPosition: "bottomleft"
        });

        if (map) {
            const cards = document.querySelectorAll(".entity-card-select");
            cards.forEach((card) => {
                if (card.dataset.lat && card.dataset.lon) {
                    addEntityMarker(card, Number(card.dataset.lat), Number(card.dataset.lon));
                }
            });
            addSeguimiento(window.DONANTE_SEGUIMIENTO || {});
            window.requestAnimationFrame(() => {
                map.invalidateSize();
                const markers = Array.from(markersByEntityId.values())
                    .concat(Array.from(truckMarkers.values()));
                if (depotMarker) markers.push(depotMarker);
                if (markers.length > 0) {
                    const isMobile = window.matchMedia("(max-width: 860px)").matches;
                    window.DDSIMap.fit(map, window.L.featureGroup(markers), null, {
                        paddingTopLeft: isMobile ? [32, 150] : [64, 96],
                        paddingBottomRight: isMobile ? [32, 40] : [64, 64]
                    });
                }
            });
        }
    }

    function addEntityMarker(card, latitud, longitud) {
        if (!map || !Number.isFinite(latitud) || !Number.isFinite(longitud)
                || Math.abs(latitud) > 90 || Math.abs(longitud) > 180) return null;

        const id = card.dataset.id;
        const name = card.dataset.name || "Entidad beneficiaria";
        const rubro = card.dataset.rubro;
        const address = card.querySelector(".entity-address")?.textContent.trim();
        const content = [
            `<strong>${escapeHtml(name)}</strong>`,
            rubro ? `<div>${escapeHtml(rubro)}</div>` : "",
            address ? `<div>${escapeHtml(address)}</div>` : ""
        ].join("");

        let marker = markersByEntityId.get(id);
        if (marker) {
            marker.setLatLng([latitud, longitud]);
            marker.setPopupContent(content);
        } else {
            marker = window.L.circleMarker([latitud, longitud], {
                radius: 9,
                color: "#155f2d",
                weight: 2,
                fillColor: "#eef5f0",
                fillOpacity: 1
            }).addTo(map);
            marker.bindPopup(content);
            marker.bindTooltip(name, { direction: "top", offset: [0, -8] });
            markersByEntityId.set(id, marker);
        }
        markersByEntityName.set(normalize(name), marker);
        return marker;
    }

    function addSeguimiento(data) {
        const depositoLat = data.depositoLat != null ? Number(data.depositoLat) : NaN;
        const depositoLng = data.depositoLng != null ? Number(data.depositoLng) : NaN;
        if (Number.isFinite(depositoLat) && Number.isFinite(depositoLng) && !depotMarker) {
            depotMarker = window.L.circleMarker([depositoLat, depositoLng], {
                radius: 12,
                color: "#0f1210",
                weight: 3,
                fillColor: "#1b7838",
                fillOpacity: 1
            }).addTo(map).bindPopup("<strong>" + escapeHtml(data.depositoEtiqueta || "Depósito") + "</strong><br>Punto de partida de las rutas");
            depotMarker.bindTooltip(data.depositoEtiqueta || "Depósito", { direction: "top", offset: [0, -10] });
        }

        const camiones = Array.isArray(data.camiones) ? data.camiones : [];
        camiones.forEach(function (camion) {
            const lat = camion.lat != null ? Number(camion.lat) : NaN;
            const lng = camion.lng != null ? Number(camion.lng) : NaN;
            if (!camion.patente || !Number.isFinite(lat) || !Number.isFinite(lng)) return;

            const popup = "<strong>Camión " + escapeHtml(camion.patente) + "</strong><br>"
                + "En camino a: " + escapeHtml(camion.destino || "próxima parada") + "<br>"
                + (camion.donaciones ? camion.donaciones.length + " donación(es)" : "");

            if (truckMarkers.has(camion.patente)) {
                truckMarkers.get(camion.patente).setLatLng([lat, lng]).setPopupContent(popup);
            } else {
                const icon = window.L.divIcon({
                    className: "",
                    html: '<div class="donante-truck">' + TRUCK_SVG + "</div>",
                    iconSize: [32, 32],
                    iconAnchor: [16, 16],
                    popupAnchor: [0, -18]
                });
                const marker = window.L.marker([lat, lng], { icon: icon, title: "Camión " + camion.patente })
                    .addTo(map)
                    .bindPopup(popup);
                truckMarkers.set(camion.patente, marker);
            }

            const trazado = Array.isArray(camion.trazado) ? camion.trazado : [];
            if (trazado.length >= 2) {
                const puntos = trazado.map(p => [p[1], p[0]]);
                if (truckLines.has(camion.patente)) {
                    truckLines.get(camion.patente).setLatLngs(puntos);
                } else {
                    truckLines.set(camion.patente, window.L.polyline(puntos, {
                        color: "#1b7838",
                        weight: 3,
                        opacity: 0.55,
                        dashArray: "6 6"
                    }).addTo(map));
                }
            }
        });
    }

    function normalize(value) {
        return String(value || "").trim().toLocaleLowerCase();
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function initEntityDialog() {
        const modal = document.getElementById("entitiesDialog");
        const trigger = document.getElementById("openEntities");
        const input = document.getElementById("entitiesFilter");
        const noMatch = document.getElementById("entitiesNoMatch");
        const message = document.getElementById("entitySelectionMessage");
        if (!modal || !trigger || !input) return;

        let lastFocusedElement = null;
        const cards = Array.from(modal.querySelectorAll(".entity-card-select"));

        function filterEntities() {
            const query = input.value.trim().toLocaleLowerCase();
            cards.forEach((card) => {
                card.hidden = !card.dataset.name.toLocaleLowerCase().includes(query);
            });
            if (noMatch) {
                noMatch.hidden = cards.length === 0 || cards.some((card) => !card.hidden);
            }
        }

        function open() {
            lastFocusedElement = document.activeElement;
            if (message) message.textContent = "";
            modal.classList.add("is-open");
            modal.setAttribute("aria-hidden", "false");
            input.focus();
        }

        function close() {
            modal.classList.remove("is-open");
            modal.setAttribute("aria-hidden", "true");
            if (lastFocusedElement && typeof lastFocusedElement.focus === "function") {
                lastFocusedElement.focus();
            }
        }

        function selectEntity(card) {
            const name = card.dataset.name || "La entidad";
            if (!map) {
                if (message) message.textContent = "El mapa no está disponible en este momento.";
                return;
            }

            if (!card.dataset.lat || !card.dataset.lon) {
                if (message) message.textContent =
                    `${name} todavía no tiene coordenadas. El sistema las actualizará automáticamente cuando pueda geocodificar su dirección.`;
                return;
            }

            const latitud = Number(card.dataset.lat);
            const longitud = Number(card.dataset.lon);
            const marker = addEntityMarker(card, latitud, longitud);
            if (!marker) {
                if (message) message.textContent = `La ubicación de ${name} no es válida.`;
                return;
            }

            close();
            map.flyTo(marker.getLatLng(), Math.max(map.getZoom(), 15), { duration: 0.6 });
            window.setTimeout(() => marker.openPopup(), 650);
        }

        trigger.addEventListener("click", open);
        modal.querySelectorAll("[data-close-entities]").forEach((button) => {
            button.addEventListener("click", close);
        });
        input.addEventListener("input", filterEntities);
        cards.forEach((card) => card.addEventListener("click", () => selectEntity(card)));
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && modal.classList.contains("is-open")) close();
        });

        filterEntities();
    }

    function initDonationHistory() {
        const layout = document.querySelector(".map-main");
        const sidebarToggle = document.getElementById("toggleDonorInfo");
        const list = document.getElementById("historyList");
        const search = document.getElementById("historySearch");
        const status = document.getElementById("historyStatus");
        const sort = document.getElementById("historySort");
        const inProgressToggle = document.getElementById("toggleInProgress");
        const notice = document.getElementById("historyFilterNotice");
        const mapNotice = document.getElementById("mapSelectionNotice");
        if (!layout || !sidebarToggle || !list || !search || !status || !sort || !inProgressToggle) return;

        const cards = Array.from(list.querySelectorAll(".history-card"));
        const noResults = document.getElementById("historyNoResults");
        const activeStatuses = new Set(["ASIGNADO", "LISTA_PARA_ENTREGAR", "EN_TRASLADO"]);
        let showInProgress = false;

        function focusDeliveries() {
            const markers = cards
                .filter((card) => activeStatuses.has(card.dataset.status))
                .map((card) => markersByEntityName.get(normalize(card.dataset.entity)))
                .filter(Boolean);
            const truckMarkersList = Array.from(truckMarkers.values());
            const uniqueMarkers = Array.from(new Set(markers.concat(truckMarkersList)));

            if (uniqueMarkers.length === 0) {
                if (mapNotice) {
                    mapNotice.textContent =
                        "No hay entregas en proceso con ubicaciones disponibles para mostrar.";
                }
                return;
            }

            if (mapNotice) {
                mapNotice.textContent = uniqueMarkers.length === 1
                    ? "Destino de una entrega en proceso."
                    : `Destinos de ${uniqueMarkers.length} entregas en proceso.`;
            }
            if (uniqueMarkers.length === 1) {
                const marker = uniqueMarkers[0];
                map.flyTo(marker.getLatLng(), Math.max(map.getZoom(), 14), { duration: 0.6 });
                window.setTimeout(() => marker.openPopup(), 650);
            } else {
                window.DDSIMap.fit(map, window.L.featureGroup(uniqueMarkers), [56, 56]);
            }
        }

        function updateHistory() {
            const query = normalize(search.value);
            const selectedStatus = status.value;

            cards.forEach((card) => {
                const searchableText = normalize([
                    card.dataset.name,
                    card.dataset.category,
                    card.dataset.subcategory,
                    card.dataset.entity
                ].join(" "));
                const matchesQuery = searchableText.includes(query);
                const matchesStatus = !selectedStatus || card.dataset.status === selectedStatus;
                const matchesInProgress = !showInProgress || activeStatuses.has(card.dataset.status);
                card.hidden = !(matchesQuery && matchesStatus && matchesInProgress);
            });

            cards.sort((first, second) => {
                if (sort.value === "name") {
                    return first.dataset.name.localeCompare(second.dataset.name, "es", { sensitivity: "base" });
                }
                const dateOrder = (first.dataset.date || "").localeCompare(second.dataset.date || "");
                return sort.value === "oldest" ? dateOrder : -dateOrder;
            }).forEach((card) => list.appendChild(card));

            if (noResults) {
                noResults.hidden = cards.length === 0 || cards.some((card) => !card.hidden);
            }
            if (notice) {
                notice.textContent = showInProgress
                    ? "Mostrando donaciones asignadas, listas para entregar o en traslado."
                    : "";
            }
        }

        sidebarToggle.addEventListener("click", () => {
            const collapsed = layout.classList.toggle("sidebar-collapsed");
            sidebarToggle.setAttribute("aria-expanded", String(!collapsed));
            sidebarToggle.setAttribute("aria-label", collapsed
                ? "Mostrar historial de donaciones"
                : "Ocultar historial de donaciones");
            sidebarToggle.querySelector("i").className = collapsed
                ? "bi bi-chevron-right"
                : "bi bi-chevron-left";
            const label = sidebarToggle.querySelector(".map-corner-toggle-label");
            if (label) label.textContent = collapsed ? "Historial" : "Ocultar";
            if (map) {
                window.setTimeout(() => map.invalidateSize(), 220);
            }
        });

        search.addEventListener("input", updateHistory);
        status.addEventListener("change", updateHistory);
        sort.addEventListener("change", updateHistory);
        inProgressToggle.addEventListener("click", () => {
            showInProgress = !showInProgress;
            inProgressToggle.setAttribute("aria-pressed", String(showInProgress));
            inProgressToggle.querySelector("span").textContent = showInProgress
                ? "Ver todo mi historial"
                : "Ver entregas en proceso";
            updateHistory();
            if (showInProgress && map) focusDeliveries();
            if (!showInProgress && mapNotice) mapNotice.textContent = "";
        });

        cards.forEach((card) => {
            card.addEventListener("click", () => {
                const entityName = normalize(card.dataset.entity);
                const marker = markersByEntityName.get(entityName);
                if (!mapNotice) return;
                if (!marker) {
                    mapNotice.textContent = `No hay ubicación disponible para ${card.dataset.entity || "este destino"}.`;
                    return;
                }
                mapNotice.textContent = `Destino: ${card.dataset.entity}.`;
                map.flyTo(marker.getLatLng(), Math.max(map.getZoom(), 14), { duration: 0.6 });
                window.setTimeout(() => marker.openPopup(), 650);
            });
        });

        updateHistory();
    }

    function initMapDrawers() {
        const controls = [
            {
                button: document.getElementById("toggleMapOptions"),
                panel: document.getElementById("mapOptionsPanel")
            }
        ].filter((item) => item.button && item.panel);

        function close() {
            const item = controls[0];
            if (!item) return;
            item.button.setAttribute("aria-expanded", "false");
            item.panel.setAttribute("aria-hidden", "true");
            item.panel.classList.remove("is-open");
            item.button.closest(".beneficiary-map").classList.remove("options-open");
            item.button.querySelector("i").className = "bi bi-chevron-left";
            item.button.setAttribute("aria-label", "Abrir objetivos del mapa");
            const label = item.button.querySelector(".map-corner-toggle-label");
            if (label) label.textContent = "Objetivos";
        }

        controls.forEach((item) => {
            item.button.addEventListener("click", () => {
                const isOpen = item.button.getAttribute("aria-expanded") === "true";
                if (isOpen) {
                    close();
                } else {
                    item.button.setAttribute("aria-expanded", "true");
                    item.panel.setAttribute("aria-hidden", "false");
                    item.panel.classList.add("is-open");
                    item.button.closest(".beneficiary-map").classList.add("options-open");
                    item.button.querySelector("i").className = "bi bi-chevron-right";
                    item.button.setAttribute("aria-label", "Cerrar objetivos del mapa");
                    const label = item.button.querySelector(".map-corner-toggle-label");
                    if (label) label.textContent = "Cerrar";
                }
            });
        });

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && controls[0]
                    && controls[0].button.getAttribute("aria-expanded") === "true") {
                close();
            }
        });
    }

    function init() {
        initMap();
        initEntityDialog();
        initDonationHistory();
        initMapDrawers();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
