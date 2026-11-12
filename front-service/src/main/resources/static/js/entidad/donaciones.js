/* =========================================================
   DDSI · Donaciones: búsqueda y filtro de estado (cliente)
   ========================================================= */
(function () {
    "use strict";

    const rows = Array.from(document.querySelectorAll(".donacion-row"));
    const filterText = document.getElementById("filterText");
    const filterEstado = document.getElementById("filterEstado");
    const emptyState = document.getElementById("donacionesEmpty");
    const clearFilters = document.getElementById("clearFilters");

    function aplicarFiltros() {
        const texto = (filterText && filterText.value || "").trim().toLowerCase();
        const estado = filterEstado ? filterEstado.value : "";
        let visibles = 0;

        rows.forEach(function (row) {
            const coincideTexto = !texto || (row.dataset.text || "").toLowerCase().includes(texto);
            const coincideEstado = !estado || row.dataset.estado === estado;
            const visible = coincideTexto && coincideEstado;
            row.hidden = !visible;
            if (visible) visibles++;
        });

        if (emptyState) emptyState.hidden = visibles !== 0 || rows.length === 0;
    }

    [filterText, filterEstado].forEach(function (el) {
        if (el) el.addEventListener("input", aplicarFiltros);
    });

    if (clearFilters) clearFilters.addEventListener("click", function () {
        if (filterText) filterText.value = "";
        if (filterEstado) filterEstado.value = "";
        aplicarFiltros();
    });
})();
