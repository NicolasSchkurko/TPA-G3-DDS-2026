package ar.edu.utn.frba.ddsi.front.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Puente temporal de identidad mientras el backend no vincula al usuario de Keycloak
 * con su perfil de dominio. Persiste {@code username -> { ROL: perfilId }} en un JSON local.
 *
 * <p>Pensado para demo/desarrollo. En producción este mapping debe vivir en la base de datos
 * o ser reemplazado por un endpoint del backend (ver PENDIENTES.md).
 */
@Component
public class IdentidadMappingStore {

    private static final Logger log = LoggerFactory.getLogger(IdentidadMappingStore.class);

    private final Path archivo;
    private final ObjectMapper mapper = new ObjectMapper();

    public IdentidadMappingStore(@Value("${front.entidad.mapping-file:./data/identidad-usuarios.json}") String ruta) {
        this.archivo = Path.of(ruta).toAbsolutePath().normalize();
    }

    /** Devuelve el perfil vinculado al usuario para un rol, si existe. */
    public Optional<UUID> obtener(String username, String rol) {
        if (username == null || rol == null) return Optional.empty();
        return leerTodo().entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(username.trim()))
                .findFirst()
                .flatMap(e -> parseUuid(e.getValue().get(rol.toUpperCase())));
    }

    /** Vincula (o sobrescribe) el perfil de un usuario para un rol. */
    public void guardar(String username, String rol, UUID id) {
        if (username == null || username.isBlank() || rol == null || id == null) {
            throw new IllegalArgumentException("username, rol e id son obligatorios para vincular un perfil.");
        }
        synchronized (this) {
            Map<String, Map<String, String>> data = leerTodo();
            data.computeIfAbsent(username.trim(), k -> new LinkedHashMap<>())
                    .put(rol.toUpperCase(), id.toString());
            escribir(data);
        }
    }

    /** Lectura completa del mapping (para el selector de respaldo). */
    public Map<String, Map<String, String>> leerTodo() {
        synchronized (this) {
            if (!Files.exists(archivo)) return new LinkedHashMap<>();
            try {
                byte[] bytes = Files.readAllBytes(archivo);
                if (bytes.length == 0) return new LinkedHashMap<>();
                Map<String, Map<String, String>> data = mapper.readValue(bytes, new TypeReference<>() {});
                return data == null ? new LinkedHashMap<>() : new LinkedHashMap<>(data);
            } catch (IOException e) {
                log.error("No se pudo leer el mapping de identidad en {}: {}", archivo, e.getMessage());
                return new LinkedHashMap<>();
            }
        }
    }

    private void escribir(Map<String, Map<String, String>> data) {
        try {
            Path parent = archivo.getParent();
            if (parent != null) Files.createDirectories(parent);
            Path tmp = archivo.resolveSibling(archivo.getFileName() + ".tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), data);
            Files.move(tmp, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.error("No se pudo escribir el mapping de identidad en {}: {}", archivo, e.getMessage());
            throw new UncheckedIOException("No se pudo persistir el vínculo de identidad.", e);
        }
    }

    private static Optional<UUID> parseUuid(String valor) {
        try {
            return Optional.of(UUID.fromString(valor));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
