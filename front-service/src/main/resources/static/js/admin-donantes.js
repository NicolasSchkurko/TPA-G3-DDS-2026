/**
 * Pantalla "Administrar donantes" (/admin/donantes).
 *
 *  - Buscador incremental: filtra las filas ya renderizadas a medida que se escribe, sin recargar
 *    la página ni esperar un click. El listado completo viene del servidor en el HTML inicial.
 *  - Importar CSV: el <input type="file"> está oculto y lo dispara el botón "Importar CSV";
 *    una vez elegido el archivo se muestra el nombre y recién ahí se habilita el submit.
 *  - Modal de actualización: se completa con los data-* de la fila que disparó el modal.
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
    // Modal de actualización
    // ------------------------------------------------------------------
    const modalActualizar = document.getElementById('modalActualizar');

    if (modalActualizar) {
        modalActualizar.addEventListener('show.bs.modal', function (evento) {
            const boton = evento.relatedTarget;
            if (!boton) {
                return;
            }

            const esJuridica = boton.getAttribute('data-tipo') === 'JURIDICA';

            document.getElementById('formActualizar').action =
                '/admin/donantes/' + boton.getAttribute('data-id') + '/actualizar';

            document.getElementById('inputNombre').value = boton.getAttribute('data-nombre') || '';
            document.getElementById('inputApellido').value = boton.getAttribute('data-apellido') || '';
            document.getElementById('inputRazonSocial').value = boton.getAttribute('data-razon-social') || '';
            document.getElementById('inputCorreo').value = boton.getAttribute('data-correo') || '';
            document.getElementById('inputTelefono').value = boton.getAttribute('data-telefono') || '';

            // Una persona humana no tiene razón social y una jurídica no tiene nombre/apellido:
            // mostramos sólo los campos que aplican para no mandar datos que el backend ignora.
            document.getElementById('campoNombre').hidden = esJuridica;
            document.getElementById('campoApellido').hidden = esJuridica;
            document.getElementById('campoRazonSocial').hidden = !esJuridica;
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
