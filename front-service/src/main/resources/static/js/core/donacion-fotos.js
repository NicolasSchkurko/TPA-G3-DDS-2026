(function () {
    'use strict';

    const input = document.getElementById('fotosDonacion');
    if (!input) return;

    const preview = document.getElementById('fotosDonacionPreview');
    const hiddenContainer = document.getElementById('fotosDonacionHidden');
    const errorBox = document.getElementById('fotosDonacionError');
    const form = input.closest('form');
    const csrfMeta = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
    let fotos = [];
    let pendientes = 0;

    const estadoBox = document.createElement('div');
    estadoBox.id = 'fotosDonacionEstado';
    estadoBox.className = 'text-muted small mt-2';
    estadoBox.setAttribute('role', 'status');
    estadoBox.setAttribute('aria-live', 'polite');
    estadoBox.hidden = true;
    if (preview && preview.parentNode) {
        preview.insertAdjacentElement('afterend', estadoBox);
    } else if (errorBox && errorBox.parentNode) {
        errorBox.parentNode.insertBefore(estadoBox, errorBox);
    }

    function mostrarError(mensaje) {
        if (!errorBox) return;
        errorBox.textContent = mensaje || '';
        errorBox.hidden = !mensaje;
    }

    function mostrarEstado(texto) {
        if (!texto) {
            estadoBox.hidden = true;
            estadoBox.replaceChildren();
            return;
        }
        const spinner = document.createElement('span');
        spinner.className = 'spinner-border spinner-border-sm me-1';
        spinner.setAttribute('aria-hidden', 'true');
        const etiqueta = document.createElement('span');
        etiqueta.textContent = texto;
        estadoBox.replaceChildren(spinner, etiqueta);
        estadoBox.hidden = false;
    }

    function actualizarEstado() {
        if (pendientes <= 0) {
            mostrarEstado('');
            return;
        }
        mostrarEstado(pendientes === 1 ? 'Subiendo la foto…' : 'Subiendo ' + pendientes + ' fotos…');
    }

    function render() {
        if (hiddenContainer) {
            hiddenContainer.replaceChildren(...fotos.map((url, index) => {
                const campo = document.createElement('input');
                campo.type = 'hidden';
                campo.name = 'imagenes[' + index + ']';
                campo.value = url;
                return campo;
            }));
        }
        if (preview) {
            preview.replaceChildren(...fotos.map((url, index) => {
                const figura = document.createElement('figure');
                figura.className = 'foto-donacion';
                const imagen = document.createElement('img');
                imagen.src = url;
                imagen.alt = 'Foto ' + (index + 1) + ' de la donación';
                const quitar = document.createElement('button');
                quitar.type = 'button';
                quitar.className = 'foto-donacion-quitar';
                quitar.textContent = 'Quitar';
                quitar.addEventListener('click', () => {
                    fotos.splice(index, 1);
                    render();
                });
                figura.append(imagen, quitar);
                return figura;
            }));
        }
    }

    async function subir(archivo) {
        if (!archivo.type || !archivo.type.startsWith('image/')) {
            throw new Error('Elegí archivos JPG, PNG o WEBP.');
        }
        if (archivo.size > 10 * 1024 * 1024) {
            throw new Error('Cada imagen puede pesar como máximo 10 MB.');
        }
        const datos = new FormData();
        datos.append('file', archivo);
        const headers = {};
        if (csrfMeta && csrfHeaderMeta) headers[csrfHeaderMeta.content] = csrfMeta.content;
        const respuesta = await fetch('/api/uploads/donacion', { method: 'POST', headers: headers, body: datos });
        const cuerpo = await respuesta.json().catch(() => ({}));
        if (!respuesta.ok) throw new Error(cuerpo.error || 'No se pudo subir la foto.');
        return cuerpo.url;
    }

    input.addEventListener('change', async function () {
        const archivos = Array.from(input.files || []);
        if (!archivos.length) return;
        mostrarError('');
        pendientes += archivos.length;
        actualizarEstado();
        try {
            for (const archivo of archivos) {
                const url = await subir(archivo);
                fotos.push(url);
                render();
            }
        } catch (e) {
            mostrarError(e.message || 'No se pudo subir la foto.');
        } finally {
            pendientes -= archivos.length;
            actualizarEstado();
            input.value = '';
        }
    });

    if (form) {
        form.addEventListener('submit', function (evento) {
            if (pendientes <= 0) return;
            evento.preventDefault();
            mostrarError('Esperá a que terminen de subirse las fotos.');
        });
    }
})();
