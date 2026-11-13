package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import ar.edu.utn.frba.ddsi.front.dto.AdminDTO;
import ar.edu.utn.frba.ddsi.front.dto.ChoferDTO;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.front.dto.FormularioDonanteDTO;
import ar.edu.utn.frba.ddsi.front.dto.FormularioEntidadDTO;
import ar.edu.utn.frba.ddsi.front.dto.PersonaDonanteDTO;
import ar.edu.utn.frba.ddsi.front.services.IdentidadService;
import ar.edu.utn.frba.ddsi.front.services.SeguimientoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Controller
public class ProfileController {
    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);
    private static final List<String> ORDEN_ROLES = List.of("CHOFER", "ENTIDAD", "DONANTE", "ADMIN");

    private final DonacionesClient donacionesClient;
    private final IncentivosClient incentivosClient;
    private final IdentidadService identidadService;
    private final SeguimientoService seguimientoService;
    private final LogisticasClient logisticasClient;
    private final ObjectMapper objectMapper;
    private final String keycloakIssuerUri;
    private final String keycloakClientId;
    private final String keycloakAccountReturnUri;

    public ProfileController(DonacionesClient donacionesClient,
                             IncentivosClient incentivosClient,
                             IdentidadService identidadService,
                             SeguimientoService seguimientoService,
                             LogisticasClient logisticasClient,
                             ObjectMapper objectMapper,
                             @Value("${keycloak.issuer-uri}") String keycloakIssuerUri,
                             @Value("${keycloak.client-id}") String keycloakClientId,
                             @Value("${keycloak.account-return-uri}") String keycloakAccountReturnUri) {
        this.donacionesClient = donacionesClient;
        this.incentivosClient = incentivosClient;
        this.identidadService = identidadService;
        this.seguimientoService = seguimientoService;
        this.logisticasClient = logisticasClient;
        this.objectMapper = objectMapper;
        this.keycloakIssuerUri = keycloakIssuerUri;
        this.keycloakClientId = keycloakClientId;
        this.keycloakAccountReturnUri = keycloakAccountReturnUri;
    }

    @GetMapping("/donante")
    public String donante() {
        return "redirect:/donante/mapa";
    }

    /** Alias histórico: antes apuntaba a la account console de Keycloak. */
    @GetMapping("/donante/perfil")
    public String perfilDonanteAlias() {
        return "redirect:/perfil";
    }

    @GetMapping("/perfil")
    public String perfil(Authentication authentication, HttpSession session, Model model) {
        PerfilActual actual = resolverPerfil(authentication, session);
        model.addAttribute("tipoPerfil", actual.tipo());
        model.addAttribute("puedeEditar", actual.puedeEditar());
        model.addAttribute("urlPublica", actual.urlPublica());
        model.addAttribute("cuentaSeguridadUrl", cuentaSeguridadUrl());
        model.addAttribute("usuario", nombreVisible(authentication));
        model.addAttribute("rolesDisponiblesPerfil", rolesDe(authentication));
        model.addAttribute("rolActivo", rolActivo(session, authentication));

        if (actual.donante() != null) {
            model.addAttribute("donante", actual.donante());
            model.addAttribute("nombrePerfil", actual.donante().etiqueta());
            model.addAttribute("tipoPersona", actual.donante().getTipoPersona());
            model.addAttribute("ciudad", ciudad(actual.donante()));
            cargarIncentivos(actual.donante().getId(), model);
        } else if (actual.entidad() != null) {
            model.addAttribute("entidad", actual.entidad());
            model.addAttribute("nombrePerfil", actual.entidad().etiqueta());
            model.addAttribute("ciudad", actual.entidad().getDireccion() == null
                    ? null : actual.entidad().getDireccion().getCiudad());
            model.addAttribute("necesidadesPublicas", actual.entidad().necesidadesActivas());
        } else if (actual.chofer() != null) {
            model.addAttribute("chofer", actual.chofer());
            model.addAttribute("nombrePerfil", actual.chofer().getNombre());
        } else if (actual.admin() != null) {
            model.addAttribute("admin", actual.admin());
            model.addAttribute("nombrePerfil", nombreAdmin(actual.admin()));
        } else {
            model.addAttribute("nombrePerfil", nombreVisible(authentication));
        }
        return "perfil";
    }

    @GetMapping("/perfil/{tipo}/{id}")
    public String perfilPublico(@PathVariable String tipo, @PathVariable UUID id, Model model) {
        String tipoNormalizado = tipo == null ? "" : tipo.trim().toLowerCase(Locale.ROOT);
        if ("donante".equals(tipoNormalizado)) {
            PersonaDonanteDTO donante = donacionesClient.obtenerDonantePorId(id);
            if (donante == null || Boolean.FALSE.equals(donante.getActivo())) {
                return "redirect:/";
            }
            model.addAttribute("tipoPerfil", "DONANTE");
            model.addAttribute("donante", donante);
            model.addAttribute("nombrePerfil", donante.etiqueta());
            model.addAttribute("tipoPersona", donante.getTipoPersona());
            model.addAttribute("ciudad", ciudad(donante));
            cargarIncentivos(id, model);
        } else if ("entidad".equals(tipoNormalizado)) {
            EntidadBeneficiariaDTO entidad = donacionesClient.obtenerEntidadPorId(id);
            if (entidad == null || Boolean.FALSE.equals(entidad.getActiva())) {
                return "redirect:/";
            }
            entidad.setNecesidades(donacionesClient.obtenerNecesidadesDeEntidad(id));
            model.addAttribute("tipoPerfil", "ENTIDAD");
            model.addAttribute("entidad", entidad);
            model.addAttribute("nombrePerfil", entidad.etiqueta());
            model.addAttribute("ciudad", entidad.getDireccion() == null
                    ? null : entidad.getDireccion().getCiudad());
            model.addAttribute("necesidadesPublicas", entidad.necesidadesActivas());
        } else {
            return "redirect:/";
        }
        model.addAttribute("perfilPublico", true);
        model.addAttribute("puedeEditar", false);
        return "perfil-publico";
    }

    @GetMapping("/perfil/editar")
    public String editar(Authentication authentication, HttpSession session) {
        PerfilActual actual = resolverPerfil(authentication, session);
        if (actual.donante() != null) return "redirect:/perfil/donante/editar";
        if (actual.entidad() != null) return "redirect:/perfil/entidad/editar";
        if (actual.chofer() != null) return "redirect:/perfil/chofer/editar";
        return "redirect:/perfil";
    }

    @GetMapping("/perfil/donante/editar")
    public String editarDonante(Authentication authentication, Model model) {
        PersonaDonanteDTO donante = donanteActual(authentication);
        if (donante == null) return "redirect:/perfil";
        prepararFormularioDonante(donante, model);
        return "admin/donante-formulario";
    }

    @PostMapping("/perfil/donante/editar")
    public String guardarDonante(@ModelAttribute("formulario") FormularioDonanteDTO formulario,
                                 Authentication authentication, Model model,
                                 RedirectAttributes flash) {
        PersonaDonanteDTO donante = donanteActual(authentication);
        if (donante == null) return "redirect:/perfil";
        try {
            donacionesClient.actualizarDonanteCampos(donante.getId(), formulario.aPayload());
            flash.addFlashAttribute("toastTipo", "success");
            flash.addFlashAttribute("toastMensaje", "Tu perfil se actualizó correctamente.");
            return "redirect:/perfil";
        } catch (Exception e) {
            log.warn("No se pudo actualizar el perfil del donante {}: {}", donante.getId(), e.getMessage());
            prepararFormularioDonante(donante, model);
            model.addAttribute("formulario", formulario);
            model.addAttribute("mediosPrecargados", formulario.mediosPrecargados());
            model.addAttribute("representantesPrecargados", formulario.representantesPrecargados());
            model.addAttribute("error", mensajeError(e, "No se pudo actualizar tu perfil."));
            return "admin/donante-formulario";
        }
    }

    @GetMapping("/perfil/entidad/editar")
    public String editarEntidad(Authentication authentication, Model model) {
        EntidadBeneficiariaDTO entidad = entidadActual(authentication);
        if (entidad == null) return "redirect:/perfil";
        prepararFormularioEntidad(entidad, model);
        return "admin/entidad-formulario";
    }

    @PostMapping("/perfil/entidad/editar")
    public String guardarEntidad(@ModelAttribute("formulario") FormularioEntidadDTO formulario,
                                 Authentication authentication, Model model,
                                 RedirectAttributes flash) {
        EntidadBeneficiariaDTO entidad = entidadActual(authentication);
        if (entidad == null) return "redirect:/perfil";
        try {
            donacionesClient.actualizarEntidad(entidad.getId(), formulario.aPayload());
            flash.addFlashAttribute("toastTipo", "success");
            flash.addFlashAttribute("toastMensaje", "Los datos de tu entidad se actualizaron correctamente.");
            return "redirect:/perfil";
        } catch (Exception e) {
            log.warn("No se pudo actualizar el perfil de la entidad {}: {}", entidad.getId(), e.getMessage());
            prepararFormularioEntidad(entidad, model);
            model.addAttribute("formulario", formulario);
            model.addAttribute("error", mensajeError(e, "No se pudieron actualizar los datos de tu entidad."));
            return "admin/entidad-formulario";
        }
    }

    @GetMapping("/perfil/chofer/editar")
    public String editarChofer(Authentication authentication, Model model) {
        ChoferDTO chofer = choferActual(authentication);
        if (chofer == null) return "redirect:/perfil";
        model.addAttribute("chofer", chofer);
        return "perfil-chofer-editar";
    }

    @PostMapping("/perfil/chofer/editar")
    public String guardarChofer(@RequestParam("nombre") String nombre,
                                Authentication authentication,
                                Model model,
                                RedirectAttributes flash) {
        ChoferDTO chofer = choferActual(authentication);
        if (chofer == null) return "redirect:/perfil";
        if (nombre == null || nombre.isBlank()) {
            model.addAttribute("chofer", chofer);
            model.addAttribute("error", "El nombre del chofer no puede quedar vacío.");
            return "perfil-chofer-editar";
        }
        chofer.setNombre(nombre.trim());
        try {
            logisticasClient.actualizarChofer(chofer);
            flash.addFlashAttribute("toastTipo", "success");
            flash.addFlashAttribute("toastMensaje", "Tus datos de chofer se actualizaron correctamente.");
            return "redirect:/perfil";
        } catch (Exception e) {
            log.warn("No se pudo actualizar el chofer {}: {}", chofer.getIdChofer(), e.getMessage());
            model.addAttribute("chofer", chofer);
            model.addAttribute("error", "No se pudieron actualizar tus datos de chofer. Intentá nuevamente.");
            return "perfil-chofer-editar";
        }
    }

    @GetMapping("/donante/mapa")
    public String mapaDonante(Authentication authentication, Model model) {
        model.addAttribute("nombreDonante", nombreDonante(authentication));
        model.addAttribute("entidades", donacionesClient.obtenerEntidades());
        UUID idDonante = identidadService.donanteIdActual(authentication).orElse(null);
        cargarIncentivosDonante(idDonante, model);
        List<DonacionDTO> historial = idDonante == null
                ? List.of()
                : donacionesClient.obtenerDonacionesDeDonante(idDonante);
        model.addAttribute("historialDonaciones", historial);
        model.addAttribute("seguimientoDonanteJson", toJson(seguimientoService.construirDonante(historial)));
        return "donante/mapa";
    }

    private void prepararFormularioDonante(PersonaDonanteDTO donante, Model model) {
        model.addAttribute("formulario", FormularioDonanteDTO.desde(donante));
        model.addAttribute("mediosPrecargados", donante.getMediosDeContacto());
        model.addAttribute("representantesPrecargados", donante.getRepresentantes());
        model.addAttribute("modoPerfil", true);
        model.addAttribute("edicion", true);
        model.addAttribute("accion", "/perfil/donante/editar");
        model.addAttribute("volverA", "/perfil");
    }

    private void prepararFormularioEntidad(EntidadBeneficiariaDTO entidad, Model model) {
        model.addAttribute("formulario", FormularioEntidadDTO.desde(entidad));
        model.addAttribute("modoPerfil", true);
        model.addAttribute("edicion", true);
        model.addAttribute("accion", "/perfil/entidad/editar");
        model.addAttribute("volverA", "/perfil");
    }

    private PersonaDonanteDTO donanteActual(Authentication authentication) {
        UUID id = identidadService.donanteIdActual(authentication).orElse(null);
        return id == null ? null : donacionesClient.obtenerDonantePorId(id);
    }

    private EntidadBeneficiariaDTO entidadActual(Authentication authentication) {
        UUID id = identidadService.entidadIdActual(authentication).orElse(null);
        if (id == null) return null;
        EntidadBeneficiariaDTO entidad = donacionesClient.obtenerEntidadPorId(id);
        if (entidad != null) entidad.setNecesidades(donacionesClient.obtenerNecesidadesDeEntidad(id));
        return entidad;
    }

    private PerfilActual resolverPerfil(Authentication authentication, HttpSession session) {
        String activo = rolActivo(session, authentication);
        PerfilActual porActivo = resolverParaRol(authentication, activo);
        if (porActivo != null) {
            return porActivo;
        }
        for (String rol : rolesDe(authentication)) {
            PerfilActual candidato = resolverParaRol(authentication, rol);
            if (candidato != null) {
                return candidato;
            }
        }
        return new PerfilActual("SIN_PERFIL", null, null, null, null, null, false);
    }

    private PerfilActual resolverParaRol(Authentication authentication, String rol) {
        if (rol == null) return null;
        return switch (rol) {
            case "DONANTE" -> {
                if (!hasRole(authentication, "DONANTE")) yield null;
                PersonaDonanteDTO donante = donanteActual(authentication);
                yield donante == null ? null
                        : new PerfilActual("DONANTE", donante, null, null, null,
                                "/perfil/donante/" + donante.getId(), true);
            }
            case "ENTIDAD" -> {
                if (!hasRole(authentication, "ENTIDAD")) yield null;
                EntidadBeneficiariaDTO entidad = entidadActual(authentication);
                yield entidad == null ? null
                        : new PerfilActual("ENTIDAD", null, entidad, null, null,
                                "/perfil/entidad/" + entidad.getId(), true);
            }
            case "CHOFER" -> {
                if (!hasRole(authentication, "CHOFER")) yield null;
                ChoferDTO chofer = choferActual(authentication);
                yield chofer == null ? null
                        : new PerfilActual("CHOFER", null, null, chofer, null, null, true);
            }
            case "ADMIN" -> {
                if (!hasRole(authentication, "ADMIN")) yield null;
                UUID idAdmin = identidadService.adminIdActual(authentication).orElse(null);
                AdminDTO admin = idAdmin == null ? null : donacionesClient.obtenerAdminPorId(idAdmin);
                yield new PerfilActual("ADMIN", null, null, null, admin, null, false);
            }
            default -> null;
        };
    }

    private ChoferDTO choferActual(Authentication authentication) {
        String email = emailDe(authentication);
        if (email == null) return null;
        return logisticasClient.obtenerChoferes().stream()
                .filter(ChoferDTO::estaActivo)
                .filter(chofer -> chofer.getEmail() != null && chofer.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);
    }

    private void cargarIncentivos(UUID idDonante, Model model) {
        cargarIncentivosDonante(idDonante, model);
    }

    private void cargarIncentivosDonante(UUID idDonante, Model model) {
        if (idDonante == null) {
            model.addAttribute("incentivosDisponibles", false);
            model.addAttribute("insignias", List.of());
            model.addAttribute("rankingTop3", List.of());
            model.addAttribute("categoriaPublica", null);
            return;
        }
        Map<String, Object> publico = incentivosClient.obtenerPerfilPublico(idDonante.toString());
        Map<String, Object> mision = incentivosClient.obtenerMision(idDonante.toString());
        int progreso = numero(mision.get("progresoActual"));
        int objetivo = numero(mision.get("progresoObjetivo"));
        int porcentaje = objetivo > 0 ? Math.min(100, Math.max(0, progreso * 100 / objetivo)) : 0;
        Map<String, Object> rankingMensual = incentivosClient.obtenerRankingActual();
        List<Map<String, Object>> top3 = primerasPosiciones(rankingMensual, 3);
        List<Map<String, Object>> insignias = incentivosClient.obtenerInsignias(idDonante.toString());

        model.addAttribute("perfilPublicoIncentivos", publico);
        model.addAttribute("categoriaPublica", publico.get("nombreCategoria"));
        model.addAttribute("misionDonante", mision);
        model.addAttribute("progresoMision", progreso);
        model.addAttribute("objetivoMision", objetivo);
        model.addAttribute("porcentajeMision", porcentaje);
        model.addAttribute("insignias", insignias);
        model.addAttribute("puestoRanking", incentivosClient.obtenerPuestoRanking(idDonante.toString()));
        model.addAttribute("rankingTop3", top3);
        model.addAttribute("periodoRanking", rankingMensual.get("periodo"));
        model.addAttribute("incentivosDisponibles",
                !publico.isEmpty() || !mision.isEmpty() || !insignias.isEmpty() || !rankingMensual.isEmpty());
    }

    /**
     * El endpoint /api/rankings/actual devuelve un RankingMesDTO con una página en "ranking";
     * también toleramos la forma antigua que traía una lista directa.
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> primerasPosiciones(Map<String, Object> ranking, int limite) {
        Object pagina = ranking.get("ranking");
        Object contenido = pagina instanceof Map<?, ?> page ? page.get("content") : pagina;
        if (!(contenido instanceof List<?> lista)) return List.of();
        return lista.stream()
                .filter(Map.class::isInstance)
                .map(posicion -> (Map<String, Object>) posicion)
                .limit(limite)
                .toList();
    }

    private String cuentaSeguridadUrl() {
        return UriComponentsBuilder.fromUriString(keycloakIssuerUri + "/account/")
                .queryParam("referrer", keycloakClientId)
                .queryParam("referrer_uri", keycloakAccountReturnUri)
                .build()
                .encode()
                .toUriString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static String nombreAdmin(AdminDTO admin) {
        if (admin == null) return "Administración";
        if (admin.getNombreAMostrar() != null && !admin.getNombreAMostrar().isBlank()) {
            return admin.getNombreAMostrar();
        }
        String nombre = ((admin.getNombre() != null ? admin.getNombre() : "") + " "
                + (admin.getApellido() != null ? admin.getApellido() : "")).trim();
        return nombre.isBlank() ? (admin.getEmail() != null ? admin.getEmail() : "Administración") : nombre;
    }

    private static String ciudad(PersonaDonanteDTO donante) {
        return donante == null || donante.getDireccion() == null ? null : donante.getDireccion().getCiudad();
    }

    private static String nombreDonante(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String fullName = oidcUser.getFullName();
            if (fullName != null && !fullName.isBlank()) return fullName;
        }
        return authentication == null ? "Mi perfil" : authentication.getName();
    }

    private static String nombreVisible(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String fullName = oidcUser.getFullName();
            if (fullName != null && !fullName.isBlank()) return fullName;
        }
        return authentication == null ? "" : authentication.getName();
    }

    private static String emailDe(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String email = oidcUser.getEmail();
            return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private static boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) return false;
        String esperado = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> esperado.equalsIgnoreCase(authority.getAuthority()));
    }

    private static String rolActivo(HttpSession session, Authentication authentication) {
        List<String> roles = rolesDe(authentication);
        Object guardado = session == null ? null : session.getAttribute("rolActivo");
        if (guardado instanceof String rol && roles.contains(rol)) {
            return rol;
        }
        return roles.stream().findFirst().orElse("ENTIDAD");
    }

    private static List<String> rolesDe(Authentication authentication) {
        if (authentication == null) return List.of();
        java.util.Set<String> roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority != null && authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .collect(java.util.stream.Collectors.toSet());
        return ORDEN_ROLES.stream().filter(roles::contains).toList();
    }

    private static int numero(Object valor) {
        return valor instanceof Number numero ? numero.intValue() : 0;
    }

    private static String mensajeError(Exception e, String prefijo) {
        if (e instanceof IllegalArgumentException && e.getMessage() != null) {
            return e.getMessage();
        }
        return prefijo + " Intentá nuevamente.";
    }

    @GetMapping("/sin-acceso")
    public String sinAcceso() {
        return "sin-acceso";
    }

    private record PerfilActual(String tipo,
                                PersonaDonanteDTO donante,
                                EntidadBeneficiariaDTO entidad,
                                ChoferDTO chofer,
                                AdminDTO admin,
                                String urlPublica,
                                boolean puedeEditar) {
    }
}
