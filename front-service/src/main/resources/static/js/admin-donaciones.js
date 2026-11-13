/**
 * Pantalla "Administrar donaciones" (/admin/donaciones), en sus dos vistas.
 *
 *  - Vista por defecto: todas las donaciones. Cada fila se despliega para ver los bienes y trae
 *    los botones de Actualizar y Eliminar.
 *  - Vista ?vista=matchmaking: sólo las pendientes de asignación. Cada fila se despliega para ver
 *    el ranking de entidades que propuso el asignador y poder elegir una.
 *
 * Las dos comparten el mismo JS porque el comportamiento es el mismo: filtrar en vivo, desplegar
 * el panel de la fila y confirmar las acciones que no se pueden deshacer. Lo que cambia es qué
 * panel esconde cada fila, y eso se resuelve con un selector que acepta los dos.
 *
 * Todo lo que se despliega ya viene renderizado en el HTML (GET /api/donaciones devuelve los
 * bienes embebidos, y GET /api/donaciones/pendientes las propuestas), así que abrir una fila no
 * dispara ningún request.
 */
(function () {
    'use strict';

    const PANELES = '.panel-bienes, .panel-propuestas';
    const filas = Array.from(document.querySelectorAll('.fila-donacion'));

    // ------------------------------------------------------------------
    // Flecha que despliega el panel de la fila
    // ------------------------------------------------------------------

    /** Abre o cierra el panel de una fila y deja la flecha y el aria-expanded en sintonía. */
    function alternar(fila, abrir) {
        const boton = fila.querySelector('.btn-desplegar');
        const panel = fila.querySelector(PANELES);
        if (!boton || !panel) {
            return;
        }

        const expandido = abrir !== undefined ? abrir : panel.hidden;
        panel.hidden = !expandido;
        boton.setAttribute('aria-expanded', String(expandido));
        fila.classList.toggle('fila-desplegada', expandido);

        const icono = boton.querySelector('i');
        if (icono) {
            icono.classList.toggle('bi-chevron-right', !expandido);
            icono.classList.toggle('bi-chevron-down', expandido);
        }
    }

    filas.forEach(function (fila) {
        const principal = fila.querySelector('.fila-principal');
        if (!principal) {
            return;
        }

        principal.addEventListener('click', function (evento) {
            // Actualizar / Eliminar viven dentro de la fila: si el click salió de ahí el usuario
            // quiso navegar o borrar, no desplegar.
            if (evento.target.closest('.col-acciones')) {
                return;
            }
            alternar(fila);
        });
    });

    // En la vista de matchmaking la gracia es ver las propuestas, así que si hay una sola donación
    // pendiente se abre sola y el admin no tiene que buscar la flecha.
    if (filas.length === 1 && filas[0].classList.contains('fila-pendiente')) {
        alternar(filas[0], true);
    }

    // ------------------------------------------------------------------
    // Buscador (filtrado en vivo)
    // ------------------------------------------------------------------
    const buscador = document.getElementById('buscadorDonaciones');
    const botonLimpiar = document.getElementById('limpiarBusqueda');
    const sinResultados = document.getElementById('sinResultados');

    // Marcas diacríticas combinantes (U+0300..U+036F). Se arma con escapes en vez de pegar los
    // caracteres crudos para que el archivo no dependa de cómo se guarde la codificación.
    const DIACRITICOS = new RegExp('[\\u0300-\\u036f]', 'g');

    /** Minúsculas y sin tildes, para que "deposito" encuentre a "depósito". */
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
            const texto = normalizar(fila.getAttribute('data-nombre'));
            const coincide = termino === '' || texto.includes(termino);
            fila.hidden = !coincide;
            if (coincide) {
                visibles++;
            } else {
                // Una fila que se oculta se cierra: si no, al volver de la búsqueda aparece
                // desplegada sin que el usuario la haya abierto.
                alternar(fila, false);
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
        // Sin formulario alrededor, Enter no debería hacer nada.
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
    // Confirmaciones
    // ------------------------------------------------------------------
    document.querySelectorAll('.form-eliminar').forEach(function (formulario) {
        formulario.addEventListener('submit', function (evento) {
            if (!window.confirm('¿Seguro que querés eliminar esta donación? No se puede deshacer.')) {
                evento.preventDefault();
            }
        });
    });

    // Asignar dispara la entrega a logística y descuenta la necesidad de la entidad: conviene
    // confirmar antes, porque desde el front no hay vuelta atrás.
    document.querySelectorAll('.form-asignar').forEach(function (formulario) {
        formulario.addEventListener('submit', function (evento) {
            const boton = formulario.querySelector('.btn-asignar');
            const entidad = boton ? boton.getAttribute('data-entidad') : 'esta entidad';
            const mensaje = '¿Asignar la donación a ' + entidad + '? Queda lista para entregar y '
                + 'se le avisa a la entidad.';
            if (!window.confirm(mensaje)) {
                evento.preventDefault();
            }
        });
    });
})();
