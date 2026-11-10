package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.KeycloakRegistrationClient;
import ar.edu.utn.frba.ddsi.front.dto.IdentidadDTO;
import ar.edu.utn.frba.ddsi.front.services.IdentidadMappingStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * Self-service de roles: permite que una cuenta autenticada agregue los roles
 * {@code DONANTE} y {@code ENTIDAD} reutilizando su usuario y su email.
 *
 * <p>No crea cuentas de Keycloak: asigna el rol al usuario ya autenticado y refresca
 * las autoridades de la sesión para que las tabs del header se actualicen sin re-login.
 */
@Controller
@RequestMapping("/cuenta/roles")
public class CuentaRolesController {

    private static final Logger log = LoggerFactory.getLogger(CuentaRolesController.class);

    /** Roles que el usuario puede agregarse por su cuenta. CHOFER y ADMIN quedan fuera a propósito. */
    private static final List<String> ROLES_AUTOSERVICIO = List.of("DONANTE", "ENTIDAD");

    private final DonacionesClient donacionesClient;
    private final KeycloakRegistrationClient keycloakRegistrationClient;
    private final IdentidadMappingStore identidadMappingStore;

    public CuentaRolesController(DonacionesClient donacionesClient,
                                 KeycloakRegistrationClient keycloakRegistrationClient,
                                 IdentidadMappingStore identidadMappingStore) {
        this.donacionesClient = donacionesClient;
        this.keycloakRegistrationClient = keycloakRegistrationClient;
        this.identidadMappingStore = identidadMappingStore;
    }

    @GetMapping
    public String roles(Authentication authentication, Model model) {
        model.addAttribute("usuarioNombre", authentication != null ? authentication.getName() : "");
        String email = emailDe(authentication);
        model.addAttribute("emailCuenta", email != null ? email : "");

        List<String> actuales = rolesDe(authentication).stream().filter(ROLES_AUTOSERVICIO::contains).toList();
        List<String> faltantes = ROLES_AUTOSERVICIO.stream().filter(r -> !actuales.contains(r)).toList();
        model.addAttribute("rolesActuales", actuales);
        model.addAttribute("faltantes", faltantes);

        IdentidadDTO identidad = email != null ? donacionesClient.buscarIdentidadPorEmail(email) : null;
        model.addAttribute("perfilDonante", identidad != null && identidad.getDonanteId() != null);
        model.addAttribute("perfilEntidad", identidad != null && identidad.getEntidadId() != null);
        return "cuenta/roles";
    }

    @PostMapping("/{rol}")
    public String agregar(@PathVariable String rol,
                          @RequestParam Map<String, String> form,
                          Authentication authentication,
                          HttpServletRequest request,
                          HttpServletResponse response,
                          RedirectAttributes flash) {
        String solicitado = rol != null ? rol.trim().toUpperCase() : "";
        if (!ROLES_AUTOSERVICIO.contains(solicitado)) {
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", "Ese rol no está disponible para auto-registro.");
            return "redirect:/cuenta/roles";
        }

        List<String> actuales = rolesDe(authentication);
        if (actuales.contains(solicitado)) {
            flash.addFlashAttribute("toastTipo", "info");
            flash.addFlashAttribute("toastMensaje", "Tu cuenta ya tiene el rol " + etiquetaDe(solicitado) + ".");
            return "redirect:/cuenta/roles";
        }

        String email = emailDe(authentication);
        if (email == null) {
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", "Tu cuenta no tiene un correo verificado. Contactá a una persona administradora.");
            return "redirect:/cuenta/roles";
        }
        String username = authentication != null ? authentication.getName() : null;

        try {
            // Reutilizamos el perfil del rol si ya existe uno para este email; si no, lo creamos.
            IdentidadDTO identidad = donacionesClient.buscarIdentidadPorEmail(email);
            java.util.UUID perfilId = perfilPorRol(identidad, solicitado);
            if (perfilId == null) {
                Map<String, String> datos = new java.util.LinkedHashMap<>(form);
                datos.put("email", email);
                perfilId = donacionesClient.registrarUsuario(solicitado, datos);
            }

            if (username != null && perfilId != null) {
                try {
                    identidadMappingStore.guardar(username, solicitado, perfilId);
                } catch (Exception mappingError) {
                    log.warn("No se pudo guardar el vínculo de identidad para {} ({}): {}",
                            username, solicitado, mappingError.getMessage());
                }
            }

            // Asignamos el rol en Keycloak al usuario autenticado (no se crea una cuenta nueva).
            String userId = userIdDe(authentication);
            if (userId == null) {
                userId = keycloakRegistrationClient.buscarUsuarioIdPorEmail(email);
            }
            if (userId != null) {
                keycloakRegistrationClient.asignarRoles(userId, List.of(solicitado));
            } else {
                log.warn("No se encontró el usuario de Keycloak para {}; el rol solo quedó en Donaciones.", email);
            }

            refrescarAutoridades(authentication, request, response, email, solicitado);
            request.getSession().setAttribute("rolActivo", solicitado);

            flash.addFlashAttribute("toastTipo", "success");
            flash.addFlashAttribute("toastMensaje", "El rol " + etiquetaDe(solicitado) + " quedó agregado a tu cuenta.");
            return "redirect:" + destinoDe(solicitado);
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", e.getMessage());
            return "redirect:/cuenta/roles";
        } catch (RestClientResponseException e) {
            log.warn("Un servicio rechazó la adición del rol {} con estado {}", solicitado, e.getStatusCode());
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", e.getStatusCode().value() == 409
                    ? "Ya existe un perfil con esos datos (documento, CUIT o email). Revisalos e intentá de nuevo."
                    : "No se pudo agregar el rol. Revisá los datos e intentá de nuevo.");
            return "redirect:/cuenta/roles";
        } catch (Exception e) {
            log.error("Error inesperado agregando el rol {} a {}", solicitado, email, e);
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", "No se pudo agregar el rol. Intentá de nuevo.");
            return "redirect:/cuenta/roles";
        }
    }

    /** Recalcula las autoridades de la sesión (roles de Keycloak + identidad por email) y las persiste. */
    private void refrescarAutoridades(Authentication authentication,
                                      HttpServletRequest request,
                                      HttpServletResponse response,
                                      String email,
                                      String nuevoRol) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        if (authentication != null) {
            authorities.addAll(authentication.getAuthorities());
        }
        authorities.add(new SimpleGrantedAuthority("ROLE_" + nuevoRol));
        try {
            IdentidadDTO identidad = donacionesClient.buscarIdentidadPorEmail(email);
            if (identidad != null && identidad.getRoles() != null) {
                for (String role : identidad.getRoles()) {
                    if (role != null && !role.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("No se pudieron refrescar los roles de la identidad de {}: {}", email, e.getMessage());
        }

        OAuth2User principal = authentication != null ? (OAuth2User) authentication.getPrincipal() : null;
        String registrationId = authentication instanceof OAuth2AuthenticationToken oauthToken
                ? oauthToken.getAuthorizedClientRegistrationId() : "keycloak";
        OAuth2AuthenticationToken actualizado = new OAuth2AuthenticationToken(principal, List.copyOf(authorities), registrationId);
        SecurityContextHolder.getContext().setAuthentication(actualizado);
        try {
            new HttpSessionSecurityContextRepository().saveContext(SecurityContextHolder.getContext(), request, response);
        } catch (Exception e) {
            log.warn("No se pudo persistir el contexto de seguridad tras agregar el rol {}: {}", nuevoRol, e.getMessage());
        }
    }

    private static java.util.UUID perfilPorRol(IdentidadDTO identidad, String rol) {
        if (identidad == null || rol == null) return null;
        return switch (rol) {
            case "DONANTE" -> identidad.getDonanteId();
            case "ENTIDAD" -> identidad.getEntidadId();
            default -> null;
        };
    }

    private static List<String> rolesDe(Authentication authentication) {
        if (authentication == null) return List.of();
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a != null && a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .distinct()
                .toList();
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

    private static String userIdDe(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String sub = oidcUser.getSubject();
            if (sub != null && !sub.isBlank()) {
                return sub;
            }
        }
        return null;
    }

    private static String etiquetaDe(String rol) {
        return switch (rol) {
            case "DONANTE" -> "Donante";
            case "ENTIDAD" -> "Entidad beneficiaria";
            default -> rol;
        };
    }

    private static String destinoDe(String rol) {
        return switch (rol) {
            case "ADMIN" -> "/panel";
            case "DONANTE" -> "/donante";
            case "CHOFER" -> "/chofer";
            default -> "/entidad";
        };
    }
}
