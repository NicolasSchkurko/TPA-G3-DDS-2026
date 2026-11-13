/* =========================================================
   DDSI · Necesidades: filtros, catálogo y modal de alta/edición
   ========================================================= */
(function () {
    "use strict";

    const CATALOGO = (window.CATALOGO && typeof window.CATALOGO === "object") ? window.CATALOGO : {};

    /* ---------- Filtros del listado ---------- */
    const grid = document.getElementById("needGrid");
    const cards = grid ? Array.from(grid.querySelectorAll(".need-card")) : [];
    const filterText = document.getElementById("filterText");
    const filterTipo = document.getElementById("filterTipo");
    const filterEstado = document.getElementById("filterEstado");
    const emptyState = document.getElementById("needEmpty");
    const clearFilters = document.getElementById("clearFilters");

    function aplicarFiltros() {
        const texto = (filterText && filterText.value || "").trim().toLowerCase();
        const tipo = filterTipo ? filterTipo.value : "";
        const estado = filterEstado ? filterEstado.value : "activa";
        let visibles = 0;

        cards.forEach(function (card) {
            const coincideTexto = !texto || (card.dataset.text || "").toLowerCase().includes(texto);
            const coincideTipo = !tipo || card.dataset.tipo === tipo;
            const activa = card.dataset.activa === "true";
            const coincideEstado = estado === "todos" || activa;
            const visible = coincideTexto && coincideTipo && coincideEstado;
            card.hidden = !visible;
            if (visible) visibles++;
        });

        if (emptyState) emptyState.hidden = visibles !== 0 || cards.length === 0;
    }

    [filterText, filterTipo, filterEstado].forEach(function (el) {
        if (!el) return;
        el.addEventListener("input", aplicarFiltros);
        el.addEventListener("change", aplicarFiltros);
    });

    if (clearFilters) clearFilters.addEventListener("click", function () {
        if (filterText) filterText.value = "";
        if (filterTipo) filterTipo.value = "";
        if (filterEstado) filterEstado.value = "activa";
        aplicarFiltros();
    });

    aplicarFiltros();

    /* ---------- Datalists del catálogo ---------- */
    const categorias = Object.keys(CATALOGO);
    const catCategorias = document.getElementById("catCategorias");
    const catSubcategorias = document.getElementById("catSubcategorias");
    const inputCategoria = document.getElementById("nombreCategoria");
    const inputSubcategoria = document.getElementById("nombreSubcategoria");

    if (catCategorias) {
        categorias.forEach(function (categoria) {
            const option = document.createElement("option");
            option.value = categoria;
            catCategorias.appendChild(option);
        });
    }

    function repoblarSubcategorias() {
        if (!catSubcategorias) return;
        catSubcategorias.innerHTML = "";
        const cat = inputCategoria ? inputCategoria.value.trim() : "";
        const clave = categorias.find(function (c) { return c.toLowerCase() === cat.toLowerCase(); });
        const opciones = clave ? CATALOGO[clave] : [];
        opciones.forEach(function (sub) {
            const option = document.createElement("option");
            option.value = sub;
            catSubcategorias.appendChild(option);
        });
    }

    if (inputCategoria) inputCategoria.addEventListener("input", repoblarSubcategorias);

    /* ---------- Modal alta / edición ---------- */
    const modal = document.getElementById("necesidadModal");
    const form = document.getElementById("necesidadForm");
    const modalTitle = document.getElementById("necesidadModalTitle");
    const submitBtn = document.getElementById("necesidadSubmit");
    const tipoSelect = document.getElementById("tipoNecesidad");
    const plazoField = document.getElementById("plazoField");
    const plazoSelect = document.getElementById("plazoEnDias");
    const plazoCustom = document.getElementById("plazoCustom");
    const cantidadInput = document.getElementById("cantidadObjetivo");
    const cantidadError = document.getElementById("cantidadError");
    const formError = document.getElementById("formError");
    let lastFocused = null;

    function actualizarPlazoVisible() {
        if (!plazoField || !tipoSelect) return;
        const esExtraordinaria = tipoSelect.value === "EXTRAORDINARIA";
        plazoField.hidden = esExtraordinaria;
        if (plazoSelect) {
            plazoSelect.disabled = esExtraordinaria;
            if (esExtraordinaria) {
                plazoSelect.value = "";
                if (plazoCustom) { plazoCustom.hidden = true; plazoCustom.disabled = true; plazoCustom.value = ""; }
            } else {
                if (!plazoSelect.value) plazoSelect.value = "30";
                actualizarCustomVisible();
            }
        }
    }
    function esPeriodoPreset(valor) {
        return ["7","15","30","60","90","180","365"].indexOf(valor) !== -1;
    }
    function actualizarCustomVisible() {
        if (!plazoSelect || !plazoCustom) return;
        const val = plazoSelect.value;
        const esCustom = val !== "" && !esPeriodoPreset(val);
        // también mostrar si el select está en "custom"
        const mostrar = (val === "custom") || esCustom;
        plazoCustom.hidden = !mostrar;
        plazoCustom.disabled = !mostrar || plazoSelect.disabled;
        if (val === "custom") {
            sincronizarCustomOption();
            plazoCustom.focus();
        }
    }
    function sincronizarCustomOption() {
        if (!plazoSelect || !plazoCustom) return;
        const valor = parseInt(plazoCustom.value, 10);
        if (!valor || valor <= 0) return;
        const valStr = String(valor);
        let opt = Array.from(plazoSelect.options).find(function (o) { return o.value === valStr; });
        if (!opt) {
            opt = document.createElement("option");
            opt.value = valStr;
            opt.textContent = "Personalizado (" + valStr + " días)";
            const customOpt = plazoSelect.querySelector('option[value="custom"]');
            if (customOpt) plazoSelect.insertBefore(opt, customOpt);
            else plazoSelect.appendChild(opt);
        }
        plazoSelect.value = valStr;
    }

    function abrirModal(modoEdicion) {
        if (!modal) return;
        lastFocused = document.activeElement;
        modal.hidden = false;
        requestAnimationFrame(function () { modal.classList.add("is-open"); });
        const first = modal.querySelector("select, input, textarea, button:not([data-close-necesidad-modal])");
        if (first) first.focus();
    }

    function cerrarModal() {
        if (!modal) return;
        modal.classList.remove("is-open");
        window.setTimeout(function () { modal.hidden = true; }, 180);
        if (lastFocused && lastFocused.focus) lastFocused.focus();
    }

    function abrirNueva() {
        form.setAttribute("action", "/entidad/necesidades");
        modalTitle.textContent = "Nueva necesidad";
        submitBtn.textContent = "Guardar necesidad";
        form.reset();
        if (tipoSelect) tipoSelect.value = "RECURRENTE";
        if (plazoSelect) plazoSelect.value = "30";
        if (plazoCustom) { plazoCustom.value = ""; plazoCustom.hidden = true; }
        actualizarPlazoVisible();
        repoblarSubcategorias();
        abrirModal(false);
    }

    function abrirEdicion(data) {
        form.setAttribute("action", "/entidad/necesidades/" + encodeURIComponent(data.id));
        modalTitle.textContent = "Editar necesidad";
        submitBtn.textContent = "Guardar cambios";
        if (tipoSelect) tipoSelect.value = data.tipo || "RECURRENTE";
        if (inputCategoria) inputCategoria.value = data.categoria || "";
        if (inputSubcategoria) inputSubcategoria.value = data.subcategoria || "";
        const descripcion = document.getElementById("descripcion");
        if (descripcion) descripcion.value = data.descripcion || "";
        if (cantidadInput) cantidadInput.value = data.cantidad || "";
        const unidad = document.getElementById("unidadDeMedida");
        if (unidad) unidad.value = data.unidad || "UNIDADES";
        if (plazoSelect) {
            const plazoValor = data.plazo ? String(data.plazo) : "30";
            const presets = ["7","15","30","60","90","180","365"];
            const esPreset = presets.indexOf(plazoValor) !== -1;
            if (!esPreset && data.plazo) {
                // período personalizado: mostrar el input
                if (plazoCustom) plazoCustom.value = plazoValor;
                plazoSelect.value = "custom";
            } else {
                if (plazoCustom) plazoCustom.value = "";
                const existe = Array.from(plazoSelect.options).some(function (opt) { return opt.value === plazoValor; });
                if (!existe && data.plazo) {
                    const opt = document.createElement("option");
                    opt.value = plazoValor;
                    opt.textContent = "Cada " + plazoValor + " días";
                    plazoSelect.appendChild(opt);
                }
                plazoSelect.value = (data.tipo === "EXTRAORDINARIA") ? "" : plazoValor;
            }
        }
        actualizarPlazoVisible();
        repoblarSubcategorias();
        abrirModal(true);
    }

    document.querySelectorAll("[data-open-necesidad-modal]").forEach(function (btn) {
        btn.addEventListener("click", abrirNueva);
    });

    document.querySelectorAll("[data-close-necesidad-modal]").forEach(function (btn) {
        btn.addEventListener("click", cerrarModal);
    });

    if (tipoSelect) tipoSelect.addEventListener("change", actualizarPlazoVisible);
    if (plazoSelect) plazoSelect.addEventListener("change", actualizarCustomVisible);
    if (plazoCustom) plazoCustom.addEventListener("input", sincronizarCustomOption);

    // ?nueva=1 abre el modal (CTA del dashboard)
    if (new URLSearchParams(window.location.search).get("nueva") === "1") {
        abrirNueva();
    }

    // Validación del formulario
    if (form) {
        form.addEventListener("submit", function (event) {
            const cantidad = Number(cantidadInput.value);
            if (!cantidad || cantidad <= 0) {
                event.preventDefault();
                if (cantidadError) cantidadError.hidden = false;
                cantidadInput.focus();
                return;
            }
            if (cantidadError) cantidadError.hidden = true;

            // Si eligió "Otro", valida y sincroniza el período personalizado
            if (plazoSelect && plazoSelect.value === "custom") {
                const dias = parseInt(plazoCustom ? plazoCustom.value : "", 10);
                if (!dias || dias <= 0) {
                    event.preventDefault();
                    if (formError) {
                        formError.hidden = false;
                        formError.textContent = "Ingresá una cantidad de días válida para el período.";
                    }
                    if (plazoCustom) plazoCustom.focus();
                    submitBtn.disabled = false;
                    submitBtn.textContent = "Guardar necesidad";
                    return;
                }
                sincronizarCustomOption();
            }

            const categoria = (inputCategoria.value || "").trim();
            const subcategoria = (inputSubcategoria.value || "").trim();
            if (!categoria || !subcategoria) {
                event.preventDefault();
                if (formError) {
                    formError.hidden = false;
                    formError.textContent = "Elegí o escribí una categoría y una subcategoría.";
                }
                (categoria ? inputSubcategoria : inputCategoria).focus();
                return;
            }
            if (formError) formError.hidden = true;

            submitBtn.disabled = true;
            submitBtn.textContent = "Guardando…";
        });
    }

    // Botones de edición
    document.querySelectorAll(".js-edit-need").forEach(function (btn) {
        btn.addEventListener("click", function () {
            abrirEdicion({
                id: btn.dataset.id,
                tipo: btn.dataset.tipo,
                categoria: btn.dataset.categoria,
                subcategoria: btn.dataset.subcategoria,
                descripcion: btn.dataset.descripcion,
                cantidad: btn.dataset.cantidad,
                unidad: btn.dataset.unidad,
                plazo: btn.dataset.plazo
            });
        });
    });

    // Escape cierra el modal
    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && modal && !modal.hidden) cerrarModal();
    });

    /* ---------- Modal de confirmación de baja ---------- */
    const confirmModal = document.getElementById("confirmModal");
    const confirmForm = document.getElementById("confirmForm");
    let lastFocusedConfirm = null;

    function abrirConfirm(action) {
        if (!confirmModal || !confirmForm) return;
        lastFocusedConfirm = document.activeElement;
        confirmForm.setAttribute("action", action);
        confirmModal.hidden = false;
        requestAnimationFrame(function () { confirmModal.classList.add("is-open"); });
        const btn = confirmModal.querySelector(".btn-danger");
        if (btn) btn.focus();
    }

    function cerrarConfirm() {
        if (!confirmModal) return;
        confirmModal.classList.remove("is-open");
        window.setTimeout(function () { confirmModal.hidden = true; }, 180);
        if (lastFocusedConfirm && lastFocusedConfirm.focus) lastFocusedConfirm.focus();
    }

    document.querySelectorAll(".js-delete-need").forEach(function (btn) {
        btn.addEventListener("click", function () {
            abrirConfirm(btn.dataset.action);
        });
    });

    document.querySelectorAll("[data-close-confirm-modal]").forEach(function (btn) {
        btn.addEventListener("click", cerrarConfirm);
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && confirmModal && !confirmModal.hidden) cerrarConfirm();
    });
})();
