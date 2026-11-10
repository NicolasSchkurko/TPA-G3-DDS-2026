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
 * <p>La identidad estable es el {@code sub} de Keycloak: es inmutable aunque cambie el
 * email o el username. El email se usa sólo como fallback transitorio y como medio de
 * contacto para notificaciones.</p>
 */
@Service
public class IdentidadService {

    private static final Logger log = LoggerFactory.getLogger(IdentidadService.class);

    private final DonacionesClient donacionesClient;

    public IdentidadService(DonacionesClient donacionesClient) {
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
     * Resuelve el perfil del usuario para un rol. Primero por el {@code sub} de Keycloak;
     * si no hay vínculo, cae al email como fallback y auto-cura el vínculo.
     */
    public Optional<UUID> perfilIdActual(Authentication authentication, String rol) {
        if (authentication == null || rol == null || rol.isBlank()) {
            return Optional.empty();
        }
        String sub = subDe(authentication);
        if (sub == null) {
            return resolverPorEmail(authentication, rol);
        }
        IdentidadDTO identidad = donacionesClient.buscarIdentidadPorSub(sub);
        if (identidad != null) {
            UUID id = idDe(identidad, rol);
            return Optional.ofNullable(id);
        }
        return resolverPorEmail(authentication, rol, sub);
    }

    /** Identidad completa por el sub de Keycloak (o null si no se pudo resolver). */
    public Optional<IdentidadDTO> identidadPorSub(String sub) {
        if (sub == null || sub.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(donacionesClient.buscarIdentidadPorSub(sub.trim()));
    }

    /** Identidad completa por email (o null si no se pudo resolver). */
    public Optional<IdentidadDTO> identidadPorEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(donacionesClient.buscarIdentidadPorEmail(email.trim().toLowerCase()));
    }

    /** Registra el vínculo entre la cuenta de Keycloak y el perfil de dominio. */
    public void registrarVinculo(String sub, String rol, UUID perfilId) {
        if (perfilId != null && sub != null && !sub.isBlank() && rol != null && !rol.isBlank()) {
            donacionesClient.vincularIdentidad(sub.trim(), rol.trim().toUpperCase(), perfilId);
        }
    }

    /** Vincula manualmente el perfil de entidad del usuario (pantalla de onboarding). */
    public void vincularEntidad(String sub, UUID entidadId) {
        if (sub == null || sub.isBlank() || entidadId == null) {
            throw new IllegalArgumentException("Seleccioná una entidad válida para vincular tu perfil.");
        }
        donacionesClient.vincularIdentidad(sub.trim(), "ENTIDAD", entidadId);
    }

    /** Entidades disponibles para que el usuario elija cuál es la suya. */
    public List<EntidadBeneficiariaDTO> listarEntidadesParaVincular() {
        return donacionesClient.obtenerEntidades();
    }

    private Optional<UUID> resolverPorEmail(Authentication authentication, String rol) {
        return resolverPorEmail(authentication, rol, subDe(authentication));
    }

    private Optional<UUID> resolverPorEmail(Authentication authentication, String rol, String sub) {
        String email = emailDe(authentication);
        if (email == null) {
            return Optional.empty();
        }
        IdentidadDTO identidad = donacionesClient.buscarIdentidadPorEmail(email);
        if (identidad == null) {
            return Optional.empty();
        }
        UUID id = idDe(identidad, rol);
        if (id != null && sub != null) {
            try {
                donacionesClient.vincularIdentidad(sub, rol, id);
                log.info("Vínculo auto-curado para la cuenta (rol {}).", rol);
            } catch (Exception e) {
                log.warn("No se pudo auto-curar el vínculo de la cuenta (rol {}): {}", rol, e.getMessage());
            }
        }
        return Optional.ofNullable(id);
    }

    private static UUID idDe(IdentidadDTO identidad, String rol) {
        if (identidad == null || rol == null) return null;
        return switch (rol.toUpperCase()) {
            case "ENTIDAD" -> identidad.getEntidadId();
            case "DONANTE" -> identidad.getDonanteId();
            case "ADMIN" -> identidad.getAdminId();
            default -> null;
        };
    }

    private static String subDe(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String sub = oidcUser.getSubject();
            if (sub != null && !sub.isBlank()) {
                return sub.trim();
            }
        }
        return null;
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
