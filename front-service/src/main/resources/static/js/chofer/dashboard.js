/* =========================================================
   DonaTrack · Portal del chofer
   Mapa con paradas unidas + confirmación de entrega con foto.
   ========================================================= */
(function () {
    "use strict";

    /* ----------------------------- Mapa ----------------------------- */
    let mapa = null;
    const paradaMarkers = new Map();

    function prefiereMenosMovimiento() {
        return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    }

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function initMapa() {
        const container = document.getElementById("rutaMap");
        if (!container || !window.DDSIMap || !window.DDSIMap.isAvailable()) return;

        const geojson = window.CHOFER_GEOJSON;
        if (!geojson || !geojson.features || geojson.features.length === 0) return;

        const map = window.DDSIMap.create({
            container: "rutaMap",
            center: [-34.6037, -58.3816],
            zoom: 12,
            scrollWheelZoom: false
        });
        if (!map) return;
        mapa = map;

        const layer = window.DDSIMap.addGeoJson(map, geojson, {
            style: function () {
                return { color: "#1b7838", weight: 4, opacity: 0.88 };
            },
            pointToLayer: function (feature, latlng) {
                const estado = feature.properties && feature.properties.estado;
                const clase = estado === "deposito" ? " is-deposito"
                    : (estado === "actual" ? " is-actual" : (estado === "completa" ? " is-completa" : ""));
                const numero = estado === "deposito" ? "D"
                    : (feature.properties && feature.properties.parada ? feature.properties.parada : "");
                const icon = L.divIcon({
                    className: "",
                    html: '<span class="ruta-pin' + clase + '">' + escapeHtml(numero) + "</span>",
                    iconSize: [30, 30],
                    iconAnchor: [15, 15],
                    popupAnchor: [0, -16]
                });
                const marker = L.marker(latlng, { icon: icon, keyboard: true });
                const nro = Number(numero);
                if (estado !== "deposito" && Number.isFinite(nro)) {
                    paradaMarkers.set(nro, marker);
                }
                return marker;
            },
            onEachFeature: function (feature, capa) {
                if (!feature.geometry || feature.geometry.type !== "Point") return;
                const p = feature.properties || {};
                const entregadas = p.entregadas != null ? p.entregadas : 0;
                const total = p.total != null ? p.total : 0;
                if (p.estado === "deposito") {
                    capa.bindPopup("<strong>" + escapeHtml(p.direccion || "Depósito") + "</strong><br>Punto de partida de la ruta");
                    return;
                }
                capa.bindPopup(
                    "<strong>Parada " + escapeHtml(p.parada) + "</strong><br>" +
                    escapeHtml(p.direccion) + "<br>" +
                    entregadas + "/" + total + " entregadas"
                );
            }
        });

        window.DDSIMap.fit(map, layer, [32, 32]);
    }

    function enfocarParada(numero) {
        if (!mapa) return;
        const marker = paradaMarkers.get(Number(numero));
        if (!marker) return;

        const isMobile = window.matchMedia("(max-width: 1024px)").matches;
        if (isMobile) {
            const panel = document.querySelector(".map-panel");
            if (panel) {
                panel.scrollIntoView({
                    behavior: prefiereMenosMovimiento() ? "auto" : "smooth",
                    block: "center"
                });
            }
        }

        mapa.flyTo(marker.getLatLng(), Math.max(mapa.getZoom(), 16), {
            duration: prefiereMenosMovimiento() ? 0 : 0.6
        });
        window.setTimeout(function () { marker.openPopup(); }, prefiereMenosMovimiento() ? 0 : 650);
    }

    /** Click o Enter en una parada mueve y acerca el mapa a ese punto. */
    function initParadas() {
        document.querySelectorAll(".parada[data-parada]").forEach(function (parada) {
            parada.addEventListener("click", function (event) {
                if (event.target.closest("button, a")) return;
                enfocarParada(parada.dataset.parada);
            });
            parada.addEventListener("keydown", function (event) {
                if (event.target !== parada) return;
                if (event.key !== "Enter" && event.key !== " ") return;
                event.preventDefault();
                enfocarParada(parada.dataset.parada);
            });
        });

        document.querySelectorAll(".parada-map-btn[data-parada]").forEach(function (button) {
            button.addEventListener("click", function () {
                enfocarParada(button.dataset.parada);
            });
        });
    }

    /* ----------------------------- Modal de entrega ----------------------------- */
    function initEntrega() {
        const modal = document.getElementById("entregaModal");
        const form = document.getElementById("entregaForm");
        const input = document.getElementById("fotoInput");
        if (!modal || !form || !input) return;

        const preview = document.getElementById("photoPreview");
        const hidden = document.getElementById("fotoBase64");
        const submitBtn = document.getElementById("entregaSubmit");
        const resumen = document.getElementById("entregaResumen");
        const dropLabel = document.getElementById("photoDropLabel");
        const closeButtons = modal.querySelectorAll("[data-close-entrega]");
        const MAX_DIM = 1280;
        const JPEG_QUALITY = 0.78;
        let lastFocused = null;

        function abrir(trigger) {
            lastFocused = document.activeElement;
            form.action = trigger.dataset.action;
            const base = trigger.dataset.resumen || "Confirmá la entrega del paquete.";
            if (resumen) resumen.textContent = base + " · Adjuntá una foto como comprobante.";
            form.reset();
            hidden.value = "";
            preview.hidden = true;
            preview.removeAttribute("src");
            submitBtn.disabled = true;
            submitBtn.textContent = "Confirmar entrega";
            if (dropLabel) dropLabel.textContent = "Tocá para sacar o elegir una foto";
            modal.hidden = false;
            requestAnimationFrame(function () { modal.classList.add("is-open"); });
            const close = modal.querySelector(".modal-close");
            if (close) close.focus();
        }

        function cerrar() {
            modal.classList.remove("is-open");
            window.setTimeout(function () { modal.hidden = true; }, 180);
            if (lastFocused && typeof lastFocused.focus === "function") lastFocused.focus();
        }

        function habilitarEnvio(dataUrl) {
            hidden.value = dataUrl;
            preview.src = dataUrl;
            preview.hidden = false;
            submitBtn.disabled = false;
            if (dropLabel) dropLabel.textContent = "Cambiar la foto";
        }

        function mostrarError(mensaje) {
            if (window.DDToast) window.DDToast.show("error", mensaje);
            else window.alert(mensaje);
        }

        function procesarArchivo(file) {
            if (!file) return;
            if (!file.type || !file.type.startsWith("image/")) {
                mostrarError("El archivo tiene que ser una imagen (JPG o PNG).");
                return;
            }
            if (file.size > 10 * 1024 * 1024) {
                mostrarError("La imagen pesa más de 10 MB. Elegí una más liviana.");
                return;
            }

            const img = new Image();
            const objectUrl = URL.createObjectURL(file);
            img.onload = function () {
                URL.revokeObjectURL(objectUrl);
                let width = img.width;
                let height = img.height;
                if (width >= height && width > MAX_DIM) {
                    height = Math.round(height * (MAX_DIM / width));
                    width = MAX_DIM;
                } else if (height > width && height > MAX_DIM) {
                    width = Math.round(width * (MAX_DIM / height));
                    height = MAX_DIM;
                }
                const canvas = document.createElement("canvas");
                canvas.width = width;
                canvas.height = height;
                const ctx = canvas.getContext("2d");
                ctx.drawImage(img, 0, 0, width, height);
                ctx.globalCompositeOperation = "destination-over";
                ctx.fillStyle = "#ffffff";
                ctx.fillRect(0, 0, width, height);
                habilitarEnvio(canvas.toDataURL("image/jpeg", JPEG_QUALITY));
            };
            img.onerror = function () {
                URL.revokeObjectURL(objectUrl);
                mostrarError("No pudimos leer la imagen. Probá con otra.");
            };
            img.src = objectUrl;
        }

        input.addEventListener("change", function () {
            procesarArchivo(input.files && input.files[0]);
        });

        document.querySelectorAll(".entrega-trigger").forEach(function (trigger) {
            trigger.addEventListener("click", function () { abrir(trigger); });
        });

        closeButtons.forEach(function (button) {
            button.addEventListener("click", cerrar);
        });

        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape" && modal.classList.contains("is-open")) {
                cerrar();
            }
        });

        modal.addEventListener("click", function (event) {
            if (event.target === modal) cerrar();
        });

        modal.addEventListener("keydown", function (event) {
            if (event.key !== "Tab" || !modal.classList.contains("is-open")) return;
            const focusables = modal.querySelectorAll(
                'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
            );
            if (!focusables.length) return;
            const first = focusables[0];
            const last = focusables[focusables.length - 1];
            if (event.shiftKey && document.activeElement === first) {
                event.preventDefault();
                last.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
                event.preventDefault();
                first.focus();
            }
        });

        form.addEventListener("submit", function () {
            submitBtn.disabled = true;
            submitBtn.textContent = "Confirmando…";
        });
    }

    /* ----------------------------- Confirmaciones ----------------------------- */
    function initConfirmaciones() {
        document.querySelectorAll("form[data-confirm]").forEach(function (form) {
            form.addEventListener("submit", function (event) {
                if (!window.confirm(form.dataset.confirm)) {
                    event.preventDefault();
                }
            });
        });
    }

    function iniciar() {
        initMapa();
        initParadas();
        initEntrega();
        initConfirmaciones();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", iniciar);
    } else {
        iniciar();
    }
})();
