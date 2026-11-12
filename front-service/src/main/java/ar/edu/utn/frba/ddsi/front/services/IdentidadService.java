package ar.edu.utn.frba.ddsi.front.services;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.front.dto.IdentidadDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resuelve el perfil de dominio del usuario autenticado.
 *
 * <p>Primero intenta identificar los perfiles por el email de la cuenta (el email es el
 * puente entre Keycloak y los perfiles de dominio). Si eso no responde, usa el mapping local
 * {@code front.entidad.mapping-file} como respaldo demo.
 */
@Service
public class IdentidadService {

    private static final Logger log = LoggerFactory.getLogger(IdentidadService.class);

    private final IdentidadMappingStore store;
    private final DonacionesClient donacionesClient;

    public IdentidadService(IdentidadMappingStore store, DonacionesClient donacionesClient) {
        this.store = store;
        this.donacionesClient = donacionesClient;
    }

    /** Entidad beneficiaria del usuario autenticado, si está vinculada y el backend responde. */
    public Optional<EntidadBeneficiariaDTO> entidadActual(Authentication authentication) {
        return entidadIdActual(authentication).map(donacionesClient::obtenerEntidadPorId);
    }

    /** UUID de la entidad vinculada al usuario autenticado, si existe. */
    public Optional<UUID> entidadIdActual(Authentication authentication) {
        return perfilIdActual(authentication, "ENTIDAD");
    }

    /** UUID del donante vinculado al usuario autenticado, si existe. */
    public Optional<UUID> donanteIdActual(Authentication authentication) {
        return perfilIdActual(authentication, "DONANTE");
    }

    /** UUID del administrador vinculado al usuario autenticado, si existe. */
    public Optional<UUID> adminIdActual(Authentication authentication) {
        return perfilIdActual(authentication, "ADMIN");
    }

    /**
     * Resuelve el perfil del usuario para un rol. Primero por email (fuente de verdad),
     * luego por el mapping local como respaldo.
     */
    public Optional<UUID> perfilIdActual(Authentication authentication, String rol) {
        if (authentication == null || rol == null || rol.isBlank()) {
            return Optional.empty();
        }
        String email = emailDe(authentication);
        if (email != null) {
            IdentidadDTO identidad = donacionesClient.buscarIdentidadPorEmail(email);
            if (identidad != null) {
                UUID id = switch (rol.toUpperCase()) {
                    case "ENTIDAD" -> identidad.getEntidadId();
                    case "DONANTE" -> identidad.getDonanteId();
                    case "ADMIN" -> identidad.getAdminId();
                    default -> null;
                };
                if (id != null) {
                    guardarVinculo(authentication.getName(), rol, id);
                    return Optional.of(id);
                }
                return Optional.empty();
            }
            log.debug("No se pudo resolver la identidad por email {}; se usa el mapping local.", email);
        }
        return store.obtener(usernameDe(authentication), rol);
    }

    /** Identidad completa por email (o null si no se pudo resolver). */
    public Optional<IdentidadDTO> identidadPorEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(donacionesClient.buscarIdentidadPorEmail(email.trim().toLowerCase()));
    }

    /** Registra el vínculo username→perfil al dar de alta un perfil desde el registro web. */
    public void registrarVinculo(String username, String rol, UUID perfilId) {
        if (perfilId != null && username != null && !username.isBlank() && rol != null) {
            store.guardar(username, rol, perfilId);
        }
    }

    /** Vincula manualmente el perfil de entidad del usuario (pantalla de onboarding). */
    public void vincularEntidad(String username, UUID entidadId) {
        if (username == null || username.isBlank() || entidadId == null) {
            throw new IllegalArgumentException("Seleccioná una entidad válida para vincular tu perfil.");
        }
        store.guardar(username, "ENTIDAD", entidadId);
    }

    /** Entidades disponibles para que el usuario elija cuál es la suya (respaldo demo). */
    public List<EntidadBeneficiariaDTO> listarEntidadesParaVincular() {
        return donacionesClient.obtenerEntidades();
    }

    private void guardarVinculo(String username, String rol, UUID perfilId) {
        if (username == null || username.isBlank() || perfilId == null) {
            return;
        }
        try {
            store.guardar(username, rol, perfilId);
        } catch (Exception e) {
            log.debug("No se pudo actualizar el mapping local para {} ({}): {}", username, rol, e.getMessage());
        }
    }

    private static String emailDe(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String email = oidcUser.getEmail();
            if (email != null && !email.isBlank()) {
                return email.trim().toLowerCase();
            }
        }
        return null;
    }

    public static String usernameDe(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }
}
