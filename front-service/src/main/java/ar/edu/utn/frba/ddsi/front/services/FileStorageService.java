package ar.edu.utn.frba.ddsi.front.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Sube las imágenes a un alojamiento externo (catbox.moe por defecto) y devuelve la URL pública.
 * Así el front no guarda archivos en disco y los servicios de dominio sólo persisten la URL.
 */
@Service
public class FileStorageService {

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> CONTENT_TYPES_PERMITIDOS =
            Set.of("image/jpeg", "image/png", "image/webp");
    private static final long TAMANIO_MAXIMO = 10L * 1024 * 1024;

    private final RestClient restClient;
    private final String uploadUrl;

    public FileStorageService(@Value("${app.imagenes.upload-url}") String uploadUrl) {
        // Timeout más largo que el del RestClient general: subir hasta 10 MB puede tardar.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.uploadUrl = uploadUrl;
    }

    /** Sube un archivo recibido por multipart. */
    public String guardarImagen(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Elegí una imagen para subir.");
        }
        if (archivo.getSize() > TAMANIO_MAXIMO) {
            throw new IllegalArgumentException("La imagen supera el máximo de 10 MB.");
        }
        String contentType = archivo.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("El archivo tiene que ser una imagen.");
        }

        String extension = extensionDe(archivo.getOriginalFilename(), contentType);
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new IllegalArgumentException("Formato no permitido. Usá JPG, PNG o WEBP.");
        }

        return subir(contenido(archivo), UUID.randomUUID() + "." + extension);
    }

    /** Sube una imagen que llega como data URL base64 (por ejemplo, el comprobante de entrega). */
    public String guardarImagenDesdeDataUrl(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) {
            throw new IllegalArgumentException("Adjuntá una foto.");
        }
        int coma = dataUrl.indexOf(',');
        if (coma < 0 || !dataUrl.regionMatches(true, 0, "data:", 0, 5)) {
            throw new IllegalArgumentException("La foto no tiene un formato válido.");
        }

        String cabecera = dataUrl.substring(5, coma);
        int puntoYComa = cabecera.indexOf(';');
        String contentType = (puntoYComa >= 0 ? cabecera.substring(0, puntoYComa) : cabecera)
                .toLowerCase(Locale.ROOT);
        if (!CONTENT_TYPES_PERMITIDOS.contains(contentType)) {
            throw new IllegalArgumentException("Formato no permitido. Usá JPG, PNG o WEBP.");
        }

        byte[] contenido;
        try {
            contenido = Base64.getDecoder().decode(dataUrl.substring(coma + 1));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La foto no tiene un formato válido.", e);
        }
        if (contenido.length > TAMANIO_MAXIMO) {
            throw new IllegalArgumentException("La imagen supera el máximo de 10 MB.");
        }

        String extension = switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
        return subir(contenido, UUID.randomUUID() + "." + extension);
    }

    private String subir(byte[] contenido, String nombre) {
        MultiValueMap<String, Object> cuerpo = new LinkedMultiValueMap<>();
        cuerpo.add("reqtype", "fileupload");
        cuerpo.add("fileToUpload", new ByteArrayResource(contenido) {
            @Override
            public String getFilename() {
                return nombre;
            }
        });

        String respuesta;
        try {
            respuesta = restClient.post()
                    .uri(uploadUrl)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(cuerpo)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo subir la imagen al alojamiento externo.", e);
        }

        String url = respuesta == null ? "" : respuesta.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalStateException("El alojamiento de imágenes rechazó el archivo: " + recortar(url));
        }
        return url;
    }

    private static String recortar(String texto) {
        return texto.length() <= 200 ? texto : texto.substring(0, 200) + "...";
    }

    private static byte[] contenido(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la imagen.", e);
        }
    }

    private static String extensionDe(String nombreOriginal, String contentType) {
        if (nombreOriginal != null && nombreOriginal.contains(".")) {
            String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf('.') + 1)
                    .toLowerCase(Locale.ROOT);
            if (!extension.isBlank()) return extension;
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }
}
