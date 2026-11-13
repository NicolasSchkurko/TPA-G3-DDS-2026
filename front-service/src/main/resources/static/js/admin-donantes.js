/**
 * Pantalla "Administrar donantes" (/admin/donantes).
 *
 *  - Buscador incremental: filtra las filas ya renderizadas a medida que se escribe, sin recargar
 *    la página ni esperar un click. El listado completo viene del servidor en el HTML inicial.
 *  - Importar CSV: el <input type="file"> está oculto y lo dispara el botón "Importar CSV";
 *    una vez elegido el archivo se muestra el nombre y recién ahí se habilita el submit.
 *  - Confirmación antes de dar de baja a un donante.
 *
 * El alta y la edición viven en /admin/donantes/nuevo y /admin/donantes/{id}/editar, con su propio
 * script (admin-donante-formulario.js).
 */
(function () {
    'use strict';

    // ------------------------------------------------------------------
    // Buscador por nombre (filtrado en vivo)
    // ------------------------------------------------------------------
    const buscador = document.getElementById('buscadorDonantes');
    const botonLimpiar = document.getElementById('limpiarBusqueda');
    const sinResultados = document.getElementById('sinResultados');
    const filas = Array.from(document.querySelectorAll('.fila-donante'));

    // Marcas diacríticas combinantes (U+0300..U+036F). Se arma con escapes en vez de pegar los
    // caracteres crudos para que el archivo no dependa de cómo se guarde la codificación.
    const DIACRITICOS = new RegExp('[\\u0300-\\u036f]', 'g');

    /** Minúsculas y sin tildes, para que "Perez" encuentre a "Pérez". */
    function normalizar(texto) {
        return (texto || '')
            .toLowerCase()
            .normalize('NFD')
            .replace(DIACRITICOS, '');
    }

    function filtrar() {
        const termino = normalizar(buscador.value.trim());
        let visibles = 0;

        filas.forEach(function (fila) {
            const nombre = normalizar(fila.getAttribute('data-nombre'));
            const coincide = termino === '' || nombre.includes(termino);
            fila.hidden = !coincide;
            if (coincide) {
                visibles++;
            }
        });

        if (sinResultados) {
            sinResultados.hidden = !(filas.length > 0 && visibles === 0);
        }
        if (botonLimpiar) {
            botonLimpiar.hidden = buscador.value === '';
        }
    }

    if (buscador) {
        buscador.addEventListener('input', filtrar);
        // Evita que Enter mande el formulario de importación que está al lado.
        buscador.addEventListener('keydown', function (evento) {
            if (evento.key === 'Enter') {
                evento.preventDefault();
            }
        });
    }

    if (botonLimpiar) {
        botonLimpiar.addEventListener('click', function () {
            buscador.value = '';
            filtrar();
            buscador.focus();
        });
    }

    // ------------------------------------------------------------------
    // Importar CSV
    // ------------------------------------------------------------------
    const botonElegirCsv = document.getElementById('botonElegirCsv');
    const inputCsv = document.getElementById('inputCsv');
    const archivoElegido = document.getElementById('archivoElegido');
    const nombreArchivo = document.getElementById('nombreArchivo');
    const cancelarCsv = document.getElementById('cancelarCsv');
    const formImportar = document.getElementById('formImportarCsv');
    const panelMapeo = document.getElementById('csvMappingPanel');
    const filasMapeo = document.getElementById('csvMappingRows');
    const mapeosInput = document.getElementById('csvMapeos');
    const cerrarMapeo = document.getElementById('csvMappingCerrar');
    const botonSubirCsv = document.getElementById('botonSubirCsv');
    let encabezadosCsv = [];

    const CAMPOS_CSV = [
        {campo: 'TIPO_PERSONA', etiqueta: 'Tipo de persona', alias: ['tipo_persona', 'tipopersona', 'tipo']},
        {campo: 'TIPO_DOC', etiqueta: 'Tipo de documento', alias: ['tipo_doc', 'tipodoc', 'tipo_documento', 'tipodocumento']},
        {campo: 'DOCUMENTO', etiqueta: 'Documento / CUIT', alias: ['documento', 'nro_documento', 'numerodocumento', 'dni', 'cuit']},
        {campo: 'NOMBRE_RAZON_SOCIAL', etiqueta: 'Nombre o razón social', alias: ['nombre', 'apellido', 'nombre_completo', 'nombrecompleto', 'razon_social', 'razonsocial']},
        {campo: 'EMAIL', etiqueta: 'Email', alias: ['email', 'mail', 'correo', 'correo_electronico', 'correoelectronico']},
        {campo: 'TELEFONO', etiqueta: 'Teléfono', alias: ['telefono', 'tel', 'celular']},
        {campo: 'WHATSAPP', etiqueta: 'WhatsApp', alias: ['whatsapp', 'wpp', 'whatsapp']}
    ];

    function normalizarHeader(valor) {
        return (valor || '')
            .replace(/^\uFEFF/, '')
            .toLowerCase()
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .replace(/[^a-z0-9]/g, '');
    }

    function parsearLinea(linea) {
        const valores = [];
        let actual = '';
        let entreComillas = false;
        for (let i = 0; i < linea.length; i++) {
            const caracter = linea[i];
            if (caracter === '"') {
                if (entreComillas && linea[i + 1] === '"') {
                    actual += '"';
                    i++;
                } else {
                    entreComillas = !entreComillas;
                }
            } else if (caracter === ',' && !entreComillas) {
                valores.push(actual.trim());
                actual = '';
            } else {
                actual += caracter;
            }
        }
        valores.push(actual.trim());
        return valores;
    }

    function pintarMapeo(encabezados) {
        if (!filasMapeo || !panelMapeo) return;
        encabezadosCsv = encabezados;
        filasMapeo.innerHTML = '';
        CAMPOS_CSV.forEach(function (definicion) {
            const label = document.createElement('label');
            label.className = 'csv-mapping-field';
            const titulo = document.createElement('span');
            titulo.textContent = definicion.etiqueta;
            const select = document.createElement('select');
            select.dataset.campo = definicion.campo;
            const vacio = document.createElement('option');
            vacio.value = '';
            vacio.textContent = 'No importar';
            select.appendChild(vacio);
            encabezados.forEach(function (encabezado) {
                const option = document.createElement('option');
                option.value = encabezado;
                option.textContent = encabezado;
                if (definicion.alias.includes(normalizarHeader(encabezado))) {
                    option.selected = true;
                }
                select.appendChild(option);
            });
            label.appendChild(titulo);
            label.appendChild(select);
            filasMapeo.appendChild(label);
        });
        panelMapeo.hidden = false;
        panelMapeo.scrollIntoView({behavior: 'smooth', block: 'nearest'});
    }

    function construirMapeos() {
        if (!filasMapeo) return '';
        const mapeos = [];
        filasMapeo.querySelectorAll('select[data-campo]').forEach(function (select) {
            if (!select.value) return;
            mapeos.push({campo: select.dataset.campo, nombresColumnas: [select.value]});
        });
        return JSON.stringify(mapeos);
    }

    if (botonElegirCsv && inputCsv) {
        botonElegirCsv.addEventListener('click', function () {
            inputCsv.click();
        });

        inputCsv.addEventListener('change', function () {
            if (!(inputCsv.files && inputCsv.files.length > 0)) return;
            nombreArchivo.textContent = inputCsv.files[0].name;
            archivoElegido.hidden = false;
            botonElegirCsv.hidden = true;
            if (mapeosInput) mapeosInput.value = '';
            inputCsv.files[0].text().then(function (texto) {
                const primeraLinea = texto.split(/\r?\n/)[0] || '';
                const encabezados = parsearLinea(primeraLinea).filter(Boolean);
                if (encabezados.length > 0 && panelMapeo) pintarMapeo(encabezados);
            }).catch(function () {
                if (panelMapeo) panelMapeo.hidden = true;
            });
        });
    }

    if (formImportar) {
        formImportar.addEventListener('submit', function () {
            const mapeos = construirMapeos();
            if (mapeosInput) mapeosInput.value = mapeos === '[]' ? '' : mapeos;
        });
    }

    function limpiarImportacion() {
        if (inputCsv) inputCsv.value = '';
        if (archivoElegido) archivoElegido.hidden = true;
        if (botonElegirCsv) botonElegirCsv.hidden = false;
        if (panelMapeo) panelMapeo.hidden = true;
        if (mapeosInput) mapeosInput.value = '';
    }

    if (cancelarCsv) cancelarCsv.addEventListener('click', limpiarImportacion);
    if (cerrarMapeo) cerrarMapeo.addEventListener('click', function () {
        if (panelMapeo) panelMapeo.hidden = true;
    });
    if (botonSubirCsv && formImportar) {
        botonSubirCsv.addEventListener('click', function () {
            if (typeof formImportar.requestSubmit === 'function') {
                formImportar.requestSubmit();
            } else {
                formImportar.submit();
            }
        });
    }

    // ------------------------------------------------------------------
    // Confirmación de borrado
    // ------------------------------------------------------------------
    document.querySelectorAll('.form-eliminar').forEach(function (formulario) {
        formulario.addEventListener('submit', function (evento) {
            const fila = formulario.closest('.fila-donante');
            const nombre = fila ? fila.getAttribute('data-nombre') : 'este donante';
            if (!window.confirm('¿Seguro que querés eliminar a ' + nombre + '? Esta acción da de baja al donante.')) {
                evento.preventDefault();
            }
        });
    });
})();
