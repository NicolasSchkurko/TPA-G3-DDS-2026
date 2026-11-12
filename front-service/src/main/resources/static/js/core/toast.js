/* =========================================================
   DDSI · Toasts accesibles
   aria-live en la región; auto-dismiss; cierre manual.
   ========================================================= */
(function () {
    "use strict";

    const REGION_ID = "toastRegion";

    function region() {
        return document.getElementById(REGION_ID);
    }

    function crearToast(tipo, mensaje, titulo) {
        const div = document.createElement("div");
        div.className = "toast toast-" + (tipo || "info");
        div.setAttribute("role", "status");
        const strong = document.createElement("strong");
        strong.textContent = titulo || (tipo === "success" ? "Listo" : tipo === "error" ? "Atención" : "Aviso");
        const body = document.createElement("div");
        body.appendChild(strong);
        body.appendChild(document.createElement("br"));
        body.appendChild(document.createTextNode(mensaje));
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "toast-close";
        btn.setAttribute("aria-label", "Descartar aviso");
        btn.innerHTML = "&times;";
        div.appendChild(body);
        div.appendChild(btn);
        return div;
    }

    function programarCierre(toast, ms) {
        window.setTimeout(function () {
            toast.style.opacity = "0";
            toast.style.transform = "translateY(-6px)";
            window.setTimeout(function () { toast.remove(); }, 220);
        }, ms || 6000);
    }

    /** Muestra un toast desde JS (polling, validaciones del cliente, etc.). */
    function showToast(tipo, mensaje, titulo) {
        const contenedor = region();
        if (!contenedor) return;
        const toast = crearToast(tipo, mensaje, titulo);
        contenedor.appendChild(toast);
        programarCierre(toast, 6000);
    }

    document.addEventListener("click", function (event) {
        const btn = event.target.closest("[data-toast-close]");
        if (!btn) return;
        const toast = btn.closest(".toast");
        if (toast) toast.remove();
    });

    // Auto-dismiss de los toasts renderizados en el servidor (flash attributes)
    document.querySelectorAll("#toastRegion .toast").forEach(function (toast) {
        programarCierre(toast, 7000);
    });

    window.DDToast = { show: showToast };
})();
