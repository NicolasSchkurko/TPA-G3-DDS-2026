/* Filtro incremental y confirmaciones para las listas simples del admin. */
(function () {
    "use strict";

    const input = document.querySelector("[data-admin-search]");
    const rows = Array.from(document.querySelectorAll("[data-admin-row]"));
    const noResults = document.querySelector("[data-admin-no-results]");
    const clear = document.querySelector("[data-admin-search-clear]");
    const DIACRITICOS = new RegExp("[\\u0300-\\u036f]", "g");

    function normalizar(value) {
        return String(value || "")
            .toLowerCase()
            .normalize("NFD")
            .replace(DIACRITICOS, "");
    }

    function aplicarFiltro() {
        if (!input) return;
        const consulta = normalizar(input.value.trim());
        let visibles = 0;

        rows.forEach(function (row) {
            const coincide = !consulta || normalizar(row.dataset.search || "").includes(consulta);
            row.hidden = !coincide;
            if (coincide) visibles += 1;
        });

        if (clear) clear.hidden = consulta.length === 0;
        if (noResults) noResults.hidden = consulta.length === 0 || visibles > 0 || rows.length === 0;
    }

    if (input) {
        input.addEventListener("input", aplicarFiltro);
        aplicarFiltro();
    }

    if (clear) {
        clear.addEventListener("click", function () {
            if (!input) return;
            input.value = "";
            input.focus();
            aplicarFiltro();
        });
    }

    document.querySelectorAll("form[data-confirm]").forEach(function (form) {
        form.addEventListener("submit", function (event) {
            if (!window.confirm(form.dataset.confirm)) {
                event.preventDefault();
            }
        });
    });

    document.querySelectorAll("form[data-submit-lock]").forEach(function (form) {
        form.addEventListener("submit", function () {
            const button = form.querySelector("button[type='submit']");
            if (!button || button.disabled) return;
            button.disabled = true;
            button.textContent = form.dataset.submitLock || "Procesando...";
        });
    });
})();
