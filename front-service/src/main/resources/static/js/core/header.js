/* =========================================================
   DDSI · Cabecera global
   Menú de usuario: toggle, click afuera y Escape cierran.
   ========================================================= */
(function () {
    "use strict";

    const toggle = document.getElementById("userMenuToggle");
    const menu = document.getElementById("userMenu");
    if (!toggle || !menu) return;

    function abrir() {
        menu.hidden = false;
        toggle.setAttribute("aria-expanded", "true");
    }

    function cerrar() {
        menu.hidden = true;
        toggle.setAttribute("aria-expanded", "false");
    }

    toggle.addEventListener("click", function (event) {
        event.stopPropagation();
        menu.hidden ? abrir() : cerrar();
    });

    document.addEventListener("click", function (event) {
        if (!menu.hidden && !menu.contains(event.target)) cerrar();
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && !menu.hidden) {
            cerrar();
            toggle.focus();
        }
    });
})();
