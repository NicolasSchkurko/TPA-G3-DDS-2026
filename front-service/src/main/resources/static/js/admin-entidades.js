/**
 * Pantalla "Administrar entidades" (/admin/entidades).
 *
 *  - Buscador incremental: filtra las filas ya renderizadas a medida que se escribe, sin recargar
 *    la página ni esperar un click. El listado completo viene del servidor en el HTML inicial.
 *  - Flecha desplegable: cada fila esconde un panel con las necesidades de esa entidad, que ya
 *    vienen renderizadas en el HTML (GET /api/entidades las devuelve embebidas), así que abrir y
 *    cerrar es puro DOM: no hay request al desplegar.
 *  - Confirmación antes de dar de baja una entidad.
 *
 * El alta y la edición viven en /admin/entidades/nuevo y /admin/entidades/{id}/editar; ese
 * formulario no necesita JS porque no tiene secciones repetibles ni bloques condicionales.
 */
(function () {
    'use strict';

    const filas = Array.from(document.querySelectorAll('.fila-entidad'));

    // ------------------------------------------------------------------
    // Flecha que despliega las necesidades
    // ------------------------------------------------------------------

    /** Abre o cierra el panel de una fila y deja la flecha y el aria-expanded en sintonía. */
    function alternar(fila, abrir) {
        const boton = fila.querySelector('.btn-desplegar');
        const panel = fila.querySelector('.panel-necesidades');
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
            // Actualizar / Eliminar están dentro de la fila: si el click salió de ahí no se
            // despliega nada, porque el usuario quiso navegar o borrar.
            if (evento.target.closest('.col-acciones')) {
                return;
            }
            alternar(fila);
        });
    });

    // ------------------------------------------------------------------
    // Buscador por razón social (filtrado en vivo)
    // ------------------------------------------------------------------
    const buscador = document.getElementById('buscadorEntidades');
    const botonLimpiar = document.getElementById('limpiarBusqueda');
    const sinResultados = document.getElementById('sinResultados');

    // Marcas diacríticas combinantes (U+0300..U+036F). Se arma con escapes en vez de pegar los
    // caracteres crudos para que el archivo no dependa de cómo se guarde la codificación.
    const DIACRITICOS = new RegExp('[\\u0300-\\u036f]', 'g');

    /** Minúsculas y sin tildes, para que "Jose" encuentre a "José". */
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
    // Confirmación de borrado
    // ------------------------------------------------------------------
    document.querySelectorAll('.form-eliminar').forEach(function (formulario) {
        formulario.addEventListener('submit', function (evento) {
            const fila = formulario.closest('.fila-entidad');
            const nombre = fila ? fila.getAttribute('data-nombre') : 'esta entidad';
            if (!window.confirm('¿Seguro que querés eliminar a ' + nombre + '? Esta acción da de baja a la entidad.')) {
                evento.preventDefault();
            }
        });
    });
})();
