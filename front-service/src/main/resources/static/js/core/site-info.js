/* =========================================================
   DDSI · Información institucional
   Modal compartido por el footer global: quiénes somos,
   qué hacemos y contacto.
   ========================================================= */
(function () {
    "use strict";

    const INFO = {
        quienes: {
            eyebrow: "Quiénes somos",
            title: "Una red que conecta voluntades.",
            body: `
                <p>Somos un equipo de estudiantes de Ingeniería en Sistemas de Información de la UTN FRBA.
                Este portal nació como Trabajo Práctico de Diseño de Sistemas y hoy funciona como una
                demostración de cómo la tecnología puede ordenar la ayuda solidaria.</p>
                <p>La plataforma reúne a donantes, organizaciones sociales y equipos de logística en un
                mismo circuito: cada donación tiene un origen, un responsable y un destino visible.</p>
            `
        },
        hacemos: {
            eyebrow: "Qué hacemos",
            title: "De la donación a la entrega, sin perder el rastro.",
            body: `
                <ul>
                    <li><strong>Registramos</strong> donaciones de alimentos, ropa, útiles y medicamentos.</li>
                    <li><strong>Vinculamos</strong> cada donación con la entidad que la recibe.</li>
                    <li><strong>Coordinamos</strong> el retiro y la entrega junto al servicio logístico.</li>
                    <li><strong>Notificamos</strong> cada cambio de estado a donantes y organizaciones.</li>
                    <li><strong>Incentivamos</strong> la participación con misiones, puntos e insignias.</li>
                </ul>
            `
        },
        contacto: {
            eyebrow: "Contactanos",
            title: "Hablemos de cómo sumar.",
            body: `
                <p>¿Tenés una organización, un comercio o una idea para colaborar?</p>
                <ul>
                    <li>Correo: <a href="mailto:donaciones@ddsi.edu.ar">donaciones@ddsi.edu.ar</a></li>
                    <li>Dirección: Campus UTN FRBA, Buenos Aires</li>
                </ul>
                <p>También podés escribirnos para coordinar una entrega o sumarte como voluntario.</p>
            `
        }
    };

    function init() {
        const modal = document.getElementById("siteInfoModal");
        if (!modal) return;

        const eyebrow = document.getElementById("siteInfoEyebrow");
        const title = document.getElementById("siteInfoTitle");
        const body = document.getElementById("siteInfoBody");
        let lastFocused = null;

        function open(key) {
            const info = INFO[key];
            if (!info) return;

            lastFocused = document.activeElement;
            if (eyebrow) eyebrow.textContent = info.eyebrow;
            if (title) title.textContent = info.title;
            if (body) body.innerHTML = info.body;
            modal.classList.add("is-open");
            modal.setAttribute("aria-hidden", "false");
            document.body.classList.add("site-info-open");

            const closeButton = modal.querySelector(".site-info-close");
            if (closeButton) closeButton.focus();
        }

        function close() {
            modal.classList.remove("is-open");
            modal.setAttribute("aria-hidden", "true");
            document.body.classList.remove("site-info-open");
            if (lastFocused && typeof lastFocused.focus === "function") lastFocused.focus();
        }

        document.querySelectorAll("[data-site-info]").forEach((trigger) => {
            trigger.addEventListener("click", (event) => {
                event.preventDefault();
                open(trigger.dataset.siteInfo);
            });
        });

        modal.querySelectorAll("[data-site-info-close]").forEach((trigger) => {
            trigger.addEventListener("click", close);
        });

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && modal.classList.contains("is-open")) {
                close();
            }
        });

        modal.addEventListener("keydown", (event) => {
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
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
