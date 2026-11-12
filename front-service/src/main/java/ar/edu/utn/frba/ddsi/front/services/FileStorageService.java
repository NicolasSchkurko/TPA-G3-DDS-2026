package ar.edu.utn.frba.ddsi.front.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long TAMANIO_MAXIMO = 10L * 1024 * 1024;

    private final Path raiz;

    public FileStorageService(@Value("${app.uploads.dir:uploads}") String directorio) {
        this.raiz = Paths.get(directorio).toAbsolutePath().normalize();
    }

    public String guardarImagen(MultipartFile archivo, String carpeta) {
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

        String nombre = UUID.randomUUID() + "." + extension;
        Path destino = raiz.resolve(carpeta).resolve(nombre).normalize();
        if (!destino.startsWith(raiz)) {
            throw new IllegalArgumentException("Destino de archivo inválido.");
        }
        try {
            Files.createDirectories(destino.getParent());
            archivo.transferTo(destino);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen.", e);
        }
        return "/uploads/" + carpeta + "/" + nombre;
    }

    public String ubicacionPublica() {
        String uri = raiz.toUri().toString();
        return uri.endsWith("/") ? uri : uri + "/";
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
