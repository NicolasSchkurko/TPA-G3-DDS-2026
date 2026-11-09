(function () {
    "use strict";

    let map = null;
    const markersByEntityId = new Map();

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
        return marker;
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

        async function selectEntity(card) {
            const name = card.dataset.name || "La entidad";
            let latitud = Number(card.dataset.lat);
            let longitud = Number(card.dataset.lon);
            if (!map) {
                if (message) message.textContent = "El mapa no está disponible en este momento.";
                return;
            }

            if (!card.dataset.lat || !card.dataset.lon) {
                if (message) message.textContent = `Buscando la ubicación de ${name}...`;
                card.disabled = true;
                try {
                    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
                    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
                    const headers = {};
                    if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
                    const urlPrefix = trigger.dataset.geocodeUrlPrefix;
                    if (!urlPrefix) throw new Error("No se configuró la ruta de geocodificación.");

                    const response = await fetch(
                        `${urlPrefix}${encodeURIComponent(card.dataset.id)}/geocodificacion`,
                        { method: "POST", headers }
                    );
                    if (!response.ok) throw new Error(`Error HTTP ${response.status}`);

                    const entidad = await response.json();
                    latitud = Number(entidad.direccion?.latitud);
                    longitud = Number(entidad.direccion?.longitud);
                    if (!Number.isFinite(latitud) || !Number.isFinite(longitud)) {
                        if (message) message.textContent =
                            `No se encontró una ubicación para ${name}. Revisá que la dirección esté completa.`;
                        return;
                    }

                    card.dataset.lat = String(latitud);
                    card.dataset.lon = String(longitud);
                } catch (error) {
                    console.error("No se pudo obtener la ubicación de la entidad.", error);
                    if (message) message.textContent =
                        `No se pudo consultar la ubicación de ${name}. Intentá nuevamente.`;
                    return;
                } finally {
                    card.disabled = false;
                }
            }

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

    function initMapDrawers() {
        const controls = [
            {
                button: document.getElementById("toggleDonorInfo"),
                panel: document.getElementById("donorInfoPanel")
            },
            {
                button: document.getElementById("toggleMapOptions"),
                panel: document.getElementById("mapOptionsPanel")
            }
        ].filter((item) => item.button && item.panel);

        function close(item) {
            item.button.setAttribute("aria-expanded", "false");
            item.panel.setAttribute("aria-hidden", "true");
            item.panel.classList.remove("is-open");
            item.button.querySelector("i").className = item.button === controls[0].button
                ? "bi bi-chevron-right"
                : "bi bi-chevron-left";
        }

        controls.forEach((item, index) => {
            item.button.addEventListener("click", () => {
                const isOpen = item.button.getAttribute("aria-expanded") === "true";
                controls.forEach(close);
                if (!isOpen) {
                    item.button.setAttribute("aria-expanded", "true");
                    item.panel.setAttribute("aria-hidden", "false");
                    item.panel.classList.add("is-open");
                    item.button.querySelector("i").className = index === 0
                        ? "bi bi-chevron-left"
                        : "bi bi-chevron-right";
                }
            });
        });

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape") controls.forEach(close);
        });
    }

    function init() {
        initMap();
        initEntityDialog();
        initMapDrawers();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
