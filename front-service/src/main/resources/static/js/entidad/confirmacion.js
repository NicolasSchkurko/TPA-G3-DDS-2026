/* =========================================================
   DDSI · Confirmación de recepción con foto (base64)
   Comprime la imagen en canvas y la envía como data URL.
   ========================================================= */
(function () {
    "use strict";

    const input = document.getElementById("fotoInput");
    if (!input) return;

    const preview = document.getElementById("photoPreview");
    const hidden = document.getElementById("fotoBase64");
    const submitBtn = document.getElementById("confirmSubmit");
    const dropLabel = document.getElementById("photoDropLabel");
    const hint = document.getElementById("confirmHint");
    const MAX_DIM = 1280;
    const JPEG_QUALITY = 0.78;

    function mostrarError(mensaje) {
        if (window.DDToast) window.DDToast.show("error", mensaje);
        else window.alert(mensaje);
    }

    function habilitarEnvio(dataUrl) {
        hidden.value = dataUrl;
        preview.src = dataUrl;
        preview.hidden = false;
        submitBtn.disabled = false;
        if (dropLabel) dropLabel.textContent = "Cambiar la foto";
        if (hint) hint.textContent = "Foto lista. Ya podés confirmar la recepción.";
    }

    function procesarArchivo(file) {
        if (!file) return;
        if (!file.type || !file.type.startsWith("image/")) {
            mostrarError("El archivo tiene que ser una imagen (JPG o PNG).");
            return;
        }
        if (file.size > 10 * 1024 * 1024) {
            mostrarError("La imagen pesa más de 10 MB. Elegí una más liviana.");
            return;
        }

        const img = new Image();
        const objectUrl = URL.createObjectURL(file);
        img.onload = function () {
            URL.revokeObjectURL(objectUrl);
            let { width, height } = img;
            if (width >= height && width > MAX_DIM) {
                height = Math.round(height * (MAX_DIM / width));
                width = MAX_DIM;
            } else if (height > width && height > MAX_DIM) {
                width = Math.round(width * (MAX_DIM / height));
                height = MAX_DIM;
            }
            const canvas = document.createElement("canvas");
            canvas.width = width;
            canvas.height = height;
            const ctx = canvas.getContext("2d");
            ctx.drawImage(img, 0, 0, width, height);
            // JPEG con fondo blanco (los PNG con transparencia quedan negros si no)
            ctx.globalCompositeOperation = "destination-over";
            ctx.fillStyle = "#ffffff";
            ctx.fillRect(0, 0, width, height);
            habilitarEnvio(canvas.toDataURL("image/jpeg", JPEG_QUALITY));
        };
        img.onerror = function () {
            URL.revokeObjectURL(objectUrl);
            mostrarError("No pudimos leer la imagen. Probá con otra.");
        };
        img.src = objectUrl;
    }

    input.addEventListener("change", function () {
        procesarArchivo(input.files && input.files[0]);
    });

    const form = document.getElementById("confirmarForm");
    if (form) {
        form.addEventListener("submit", function () {
            submitBtn.disabled = true;
            input.disabled = true;
            submitBtn.textContent = "Subiendo foto y confirmando…";
            if (hint) hint.textContent = "Subiendo la foto del comprobante. No cierres esta página.";
        });
    }
})();
