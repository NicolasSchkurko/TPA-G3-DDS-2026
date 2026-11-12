/**
 * Formularios de donación: alta (/admin/donaciones/nuevo) y actualización
 * (/admin/donaciones/{id}/editar). Las dos pantallas comparten este archivo porque comparten las
 * filas de bienes; el selector de donante existe sólo en el alta y acá se saltea si no está.
 *
 *  - Filas de bienes: agregar, quitar y mantener los índices de los name. A diferencia del
 *    formulario de donante (que bindea arrays paralelos y por eso sólo depende del orden del DOM),
 *    acá Spring bindea una lista de objetos y necesita el índice explícito:
 *    bienes[0].descripcion, bienes[1].descripcion... Por eso cada campo declara su data-campo y
 *    el name se reescribe cada vez que cambia la cantidad de filas.
 *  - Dentro de cada fila, "estado (nuevo/usado)" y "fecha de vencimiento" son excluyentes: el
 *    backend elige la subclase del bien por tipoBien e ignora el campo del otro tipo, así que el
 *    que no corresponde se esconde y se deshabilita para que el navegador no lo mande.
 *  - Selector de donante: filtra en vivo y, cuando no hay coincidencias, ofrece registrarlo o ir
 *    a administrar donantes (el caso "no reconozco al donador").
 */
(function () {
    'use strict';

    // Marcas diacríticas combinantes (U+0300..U+036F), escapadas para no depender de cómo se
    // guarde la codificación del archivo.
    const DIACRITICOS = new RegExp('[\\u0300-\\u036f]', 'g');

    function normalizar(texto) {
        return (texto || '')
            .toLowerCase()
            .normalize('NFD')
            .replace(DIACRITICOS, '');
    }

    // ==================================================================
    // Filas de bienes
    // ==================================================================
    const contenedorBienes = document.getElementById('filasBienes');
    const moldeBien = document.getElementById('moldeBien');
    const botonAgregarBien = document.getElementById('agregarBien');

    /** Deja los name alineados con la posición de la fila: bienes[i].<data-campo>. */
    function renumerarBienes() {
        const filas = contenedorBienes.querySelectorAll('.fila-bien');

        filas.forEach(function (fila, indice) {
            fila.querySelectorAll('[data-campo]').forEach(function (campo) {
                campo.name = 'bienes[' + indice + '].' + campo.getAttribute('data-campo');
            });

            // Con una sola fila no se ofrece borrarla: la donación necesita al menos un bien.
            const quitar = fila.querySelector('.btn-quitar-fila');
            if (quitar) {
                quitar.hidden = filas.length === 1;
            }
        });
    }

    /**
     * Muestra el campo que corresponde al tipo de bien elegido y deshabilita el otro.
     * Deshabilitado = el navegador no lo manda, así que el backend no recibe, por ejemplo, un
     * vencimiento para un bien con estado.
     */
    function aplicarTipoDeBien(fila) {
        const selector = fila.querySelector('.selector-tipo-bien');
        if (!selector) {
            return;
        }
        const perecedero = selector.value === 'PERECEDERO';

        [['.campo-vencimiento', perecedero], ['.campo-usado', !perecedero]].forEach(function (par) {
            const bloque = fila.querySelector(par[0]);
            const visible = par[1];
            if (!bloque) {
                return;
            }
            bloque.hidden = !visible;
            bloque.querySelectorAll('input, select').forEach(function (campo) {
                campo.disabled = !visible;
            });
        });
    }

    function aplicarTipoATodas() {
        contenedorBienes.querySelectorAll('.fila-bien').forEach(aplicarTipoDeBien);
    }

    if (contenedorBienes) {

        if (moldeBien && botonAgregarBien) {
            botonAgregarBien.addEventListener('click', function () {
                const fila = moldeBien.content.firstElementChild.cloneNode(true);
                contenedorBienes.appendChild(fila);
                renumerarBienes();
                aplicarTipoDeBien(fila);

                const primerCampo = fila.querySelector('input[data-campo="descripcion"]');
                if (primerCampo) {
                    primerCampo.focus();
                }
            });
        }

        // Un solo listener delegado: las filas que agrega el molde quedan cubiertas sin recablear.
        contenedorBienes.addEventListener('click', function (evento) {
            const boton = evento.target.closest('.btn-quitar-fila');
            if (!boton) {
                return;
            }
            // Nunca se queda sin filas: el backend rechaza una donación sin bienes.
            if (contenedorBienes.querySelectorAll('.fila-bien').length <= 1) {
                return;
            }
            const fila = boton.closest('.fila-bien');
            if (fila) {
                fila.remove();
                renumerarBienes();
            }
        });

        contenedorBienes.addEventListener('change', function (evento) {
            if (!evento.target.classList.contains('selector-tipo-bien')) {
                return;
            }
            const fila = evento.target.closest('.fila-bien');
            if (fila) {
                aplicarTipoDeBien(fila);
            }
        });

        renumerarBienes();
        aplicarTipoATodas();

        const csrfMeta = document.querySelector('meta[name="_csrf"]');
        const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
        let uploadsPendientes = 0;

        function mostrarPreviewFoto(fila, url) {
            const preview = fila.querySelector('.bien-foto-preview');
            const img = preview ? preview.querySelector('img') : null;
            if (!preview || !img) return;
            if (url) {
                img.src = url;
                preview.hidden = false;
            } else {
                img.removeAttribute('src');
                preview.hidden = true;
            }
        }

        async function subirFoto(fila, archivo) {
            const oculto = fila.querySelector('.bien-foto-url');
            const error = fila.querySelector('.bien-foto-error');
            const input = fila.querySelector('.bien-foto-input');
            if (!oculto || !archivo) return;
            if (error) error.hidden = true;
            if (!archivo.type || !archivo.type.startsWith('image/')) {
                if (error) { error.textContent = 'Elegí una imagen JPG, PNG o WEBP.'; error.hidden = false; }
                if (input) input.value = '';
                return;
            }
            if (archivo.size > 10 * 1024 * 1024) {
                if (error) { error.textContent = 'La imagen supera el máximo de 10 MB.'; error.hidden = false; }
                if (input) input.value = '';
                return;
            }
            const datos = new FormData();
            datos.append('file', archivo);
            uploadsPendientes++;
            try {
                const headers = {};
                if (csrfMeta && csrfHeaderMeta) headers[csrfHeaderMeta.content] = csrfMeta.content;
                const respuesta = await fetch('/api/uploads/donacion', { method: 'POST', headers: headers, body: datos });
                const cuerpo = await respuesta.json().catch(() => ({}));
                if (!respuesta.ok) throw new Error(cuerpo.error || 'No se pudo subir la foto.');
                oculto.value = cuerpo.url;
                mostrarPreviewFoto(fila, cuerpo.url);
            } catch (e) {
                oculto.value = '';
                mostrarPreviewFoto(fila, '');
                if (input) input.value = '';
                if (error) { error.textContent = e.message || 'No se pudo subir la foto.'; error.hidden = false; }
            } finally {
                uploadsPendientes--;
            }
        }

        contenedorBienes.addEventListener('change', function (evento) {
            if (evento.target.matches('.bien-foto-input')) {
                subirFoto(evento.target.closest('.fila-bien'), evento.target.files && evento.target.files[0]);
            }
        });

        contenedorBienes.addEventListener('click', function (evento) {
            if (!evento.target.closest('.quitar-foto')) return;
            const fila = evento.target.closest('.fila-bien');
            fila.querySelector('.bien-foto-url').value = '';
            fila.querySelector('.bien-foto-input').value = '';
            mostrarPreviewFoto(fila, '');
        });

        const formulario = document.getElementById('formularioDonacion');
        if (formulario) {
            formulario.addEventListener('submit', function (evento) {
                if (uploadsPendientes <= 0) return;
                evento.preventDefault();
                let error = document.getElementById('formUploadError');
                if (!error) {
                    error = document.createElement('div');
                    error.id = 'formUploadError';
                    error.className = 'alert alert-danger';
                    error.setAttribute('role', 'alert');
                    formulario.prepend(error);
                }
                error.textContent = 'Esperá a que terminen de subirse las fotos.';
                error.hidden = false;
            });
        }

        contenedorBienes.querySelectorAll('.fila-bien').forEach(function (fila) {
            const oculto = fila.querySelector('.bien-foto-url');
            if (oculto && oculto.value) mostrarPreviewFoto(fila, oculto.value);
        });
    }

    // ==================================================================
    // Selector de donante (sólo en el alta)
    // ==================================================================
    const buscadorDonante = document.getElementById('buscadorDonante');
    const listaDonantes = document.getElementById('listaDonantes');
    const sinDonante = document.getElementById('donanteNoEncontrado');

    if (buscadorDonante && listaDonantes) {
        const opciones = Array.from(listaDonantes.querySelectorAll('.opcion-donante'));

        function filtrarDonantes() {
            const termino = normalizar(buscadorDonante.value.trim());
            let visibles = 0;

            opciones.forEach(function (opcion) {
                const texto = normalizar(opcion.getAttribute('data-nombre'));
                const coincide = termino === '' || texto.includes(termino);
                opcion.hidden = !coincide;
                if (coincide) {
                    visibles++;
                }
            });

            // El panel de "no lo encontramos" sólo tiene sentido si el admin buscó algo: con el
            // buscador vacío se ve la lista completa.
            if (sinDonante) {
                sinDonante.hidden = !(termino !== '' && visibles === 0);
            }
        }

        buscadorDonante.addEventListener('input', filtrarDonantes);
        buscadorDonante.addEventListener('keydown', function (evento) {
            // Enter dentro del buscador no debería mandar el formulario entero.
            if (evento.key === 'Enter') {
                evento.preventDefault();
            }
        });

        // Si volvemos del alta de donante con uno preseleccionado, se lo deja a la vista.
        const elegido = listaDonantes.querySelector('.opcion-donante input:checked');
        if (elegido) {
            const opcion = elegido.closest('.opcion-donante');
            if (opcion && opcion.scrollIntoView) {
                opcion.scrollIntoView({ block: 'nearest' });
            }
        }

        filtrarDonantes();
    }
})();
