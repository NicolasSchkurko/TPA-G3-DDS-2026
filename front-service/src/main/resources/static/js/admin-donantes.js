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

    if (botonElegirCsv && inputCsv) {
        botonElegirCsv.addEventListener('click', function () {
            inputCsv.click();
        });

        inputCsv.addEventListener('change', function () {
            if (inputCsv.files && inputCsv.files.length > 0) {
                nombreArchivo.textContent = inputCsv.files[0].name;
                archivoElegido.hidden = false;
                botonElegirCsv.hidden = true;
            }
        });
    }

    if (cancelarCsv) {
        cancelarCsv.addEventListener('click', function () {
            inputCsv.value = '';
            archivoElegido.hidden = true;
            botonElegirCsv.hidden = false;
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
