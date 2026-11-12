/**
 * Formulario de alta y edición de donantes (/admin/donantes/nuevo y /admin/donantes/{id}/editar).
 *
 *  - Muestra los bloques de persona humana o jurídica según el tipo elegido.
 *  - Permite agregar y quitar filas de medios de contacto y de representantes, clonando los
 *    <template> del final del HTML.
 *
 * Las filas se bindean en el servidor como arrays paralelos (medioTipo[]/medioValor[], repNombre[]
 * y compañía), así que el orden de los inputs en el DOM es lo único que importa: no hay índices que
 * mantener en los name. La excepción es el radio del medio predeterminado, cuyo value sí es la
 * posición de la fila; por eso se renumera cada vez que se agrega o se borra una.
 */
(function () {
    'use strict';

    const formulario = document.getElementById('formularioDonante');
    if (!formulario) {
        return;
    }

    // ------------------------------------------------------------------
    // Humana / jurídica
    // ------------------------------------------------------------------
    const radiosTipo = formulario.querySelectorAll('input[type="radio"][name="tipoPersona"]');
    const bloquesPorTipo = document.querySelectorAll('[data-persona]');

    function tipoElegido() {
        const marcado = formulario.querySelector('input[name="tipoPersona"]:checked');
        return marcado ? marcado.value : 'HUMANA';
    }

    function aplicarTipo() {
        const tipo = tipoElegido();
        bloquesPorTipo.forEach(function (bloque) {
            const visible = bloque.getAttribute('data-persona') === tipo;
            bloque.hidden = !visible;
            // Los campos ocultos se deshabilitan para que el navegador no los mande: así el backend
            // no recibe, por ejemplo, un apellido cargado antes de cambiar a persona jurídica.
            bloque.querySelectorAll('input, select, textarea').forEach(function (campo) {
                campo.disabled = !visible;
            });
        });
    }

    radiosTipo.forEach(function (radio) {
        radio.addEventListener('change', aplicarTipo);
    });

    // ------------------------------------------------------------------
    // Filas repetibles
    // ------------------------------------------------------------------

    const contenedorMedios = document.getElementById('filasMedios');

    /** Deja los value de los radios de "predeterminado" alineados con la posición de su fila. */
    function renumerarMedios() {
        if (!contenedorMedios) {
            return;
        }
        const filas = contenedorMedios.querySelectorAll('.fila-medio');
        let hayMarcado = false;

        filas.forEach(function (fila, indice) {
            const radio = fila.querySelector('input[name="medioPredeterminado"]');
            if (radio) {
                radio.value = String(indice);
                if (radio.checked) {
                    hayMarcado = true;
                }
            }
        });

        // Si se borró la fila que estaba marcada, el predeterminado pasa a ser el primero.
        if (!hayMarcado && filas.length > 0) {
            const primero = filas[0].querySelector('input[name="medioPredeterminado"]');
            if (primero) {
                primero.checked = true;
            }
        }
    }

    function configurarSeccionRepetible(opciones) {
        const contenedor = document.getElementById(opciones.contenedor);
        const molde = document.getElementById(opciones.molde);
        const botonAgregar = document.getElementById(opciones.botonAgregar);
        const avisoVacio = document.getElementById(opciones.avisoVacio);

        if (!contenedor || !molde || !botonAgregar) {
            return null;
        }

        function actualizarEstado() {
            const cantidad = contenedor.querySelectorAll('.fila-repetible').length;
            if (avisoVacio) {
                avisoVacio.hidden = cantidad > 0;
            }
            if (opciones.alCambiar) {
                opciones.alCambiar();
            }
        }

        function agregarFila(enfocar) {
            const fila = molde.content.firstElementChild.cloneNode(true);
            contenedor.appendChild(fila);
            actualizarEstado();
            if (enfocar) {
                const primerCampo = fila.querySelector('input[type="text"]');
                if (primerCampo) {
                    primerCampo.focus();
                }
            }
            return fila;
        }

        botonAgregar.addEventListener('click', function () {
            agregarFila(true);
        });

        // Un solo listener delegado: las filas que agrega el molde quedan cubiertas sin recablear.
        contenedor.addEventListener('click', function (evento) {
            const boton = evento.target.closest('.btn-quitar-fila');
            if (!boton) {
                return;
            }
            const fila = boton.closest('.fila-repetible');
            if (fila) {
                fila.remove();
                actualizarEstado();
            }
        });

        actualizarEstado();
        return { agregarFila: agregarFila };
    }

    const seccionMedios = configurarSeccionRepetible({
        contenedor: 'filasMedios',
        molde: 'moldeMedio',
        botonAgregar: 'agregarMedio',
        avisoVacio: 'sinMedios',
        alCambiar: function () {
            renumerarMedios();
        }
    });

    configurarSeccionRepetible({
        contenedor: 'filasRepresentantes',
        molde: 'moldeRepresentante',
        botonAgregar: 'agregarRepresentante',
        avisoVacio: 'sinRepresentantes'
    });

    // En el alta arrancamos con una fila de contacto lista para completar: sin ningún medio el
    // sistema no puede notificar al donante.
    if (seccionMedios && contenedorMedios && contenedorMedios.querySelectorAll('.fila-medio').length === 0) {
        seccionMedios.agregarFila(false);
    }

    aplicarTipo();
})();
