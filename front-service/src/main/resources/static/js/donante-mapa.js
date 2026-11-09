(function () {
    "use strict";

    let map = null;
    const markersByEntityId = new Map();
    const markersByEntityName = new Map();

    function initMap() {
        if (!window.DDSIMap || !window.DDSIMap.isAvailable()) return;

        map = window.DDSIMap.create({
            container: "beneficiaryMap",
            center: [-34.6200, -58.4300],
            zoom: 13,
            minZoom: 5,
            maxZoom: 18,
            scrollWheelZoom: false,
            zoomControlPosition: "bottomright"
        });

        if (map) {
            const cards = document.querySelectorAll(".entity-card-select");
            cards.forEach((card) => {
                if (card.dataset.lat && card.dataset.lon) {
                    addEntityMarker(card, Number(card.dataset.lat), Number(card.dataset.lon));
                }
            });
            window.requestAnimationFrame(() => {
                map.invalidateSize();
                const markers = Array.from(markersByEntityId.values());
                if (markers.length > 0) {
                    window.DDSIMap.fit(map, window.L.featureGroup(markers), [48, 48]);
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
            marker = window.L.marker([latitud, longitud], {
                title: name,
                alt: name
            }).addTo(map);
            marker.bindPopup(content);
            markersByEntityId.set(id, marker);
        }
        markersByEntityName.set(normalize(name), marker);
        return marker;
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
            const uniqueMarkers = Array.from(new Set(markers));

            if (uniqueMarkers.length === 0) {
                if (mapNotice) {
                    mapNotice.textContent =
                        "No hay destinos de entregas en proceso con ubicaciones disponibles para mostrar.";
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
            item.button.querySelector("i").className = "bi bi-chevron-left";
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
                    item.button.querySelector("i").className = "bi bi-chevron-right";
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
