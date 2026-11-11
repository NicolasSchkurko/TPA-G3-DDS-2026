package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.PersonaDonanteDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.text.Normalizer;
import java.util.Map;
import java.util.UUID;

@Controller
public class ProfileController {
    private final DonacionesClient donacionesClient;
    private final IncentivosClient incentivosClient;
    private final String keycloakIssuerUri;
    private final String keycloakClientId;
    private final String keycloakAccountReturnUri;

    public ProfileController(DonacionesClient donacionesClient,
                             IncentivosClient incentivosClient,
                             @Value("${keycloak.issuer-uri}") String keycloakIssuerUri,
                             @Value("${keycloak.client-id}") String keycloakClientId,
                             @Value("${keycloak.account-return-uri}") String keycloakAccountReturnUri) {
        this.donacionesClient = donacionesClient;
        this.incentivosClient = incentivosClient;
        this.keycloakIssuerUri = keycloakIssuerUri;
        this.keycloakClientId = keycloakClientId;
        this.keycloakAccountReturnUri = keycloakAccountReturnUri;
    }

    @GetMapping("/donante")
    public String donante() {
        return "redirect:/donante/mapa";
    }

    @GetMapping("/donante/perfil")
    public String editarPerfilDonante() {
        String cuentaKeycloak = UriComponentsBuilder.fromUriString(keycloakIssuerUri + "/account")
                .queryParam("referrer", keycloakClientId)
                .queryParam("referrer_uri", keycloakAccountReturnUri)
                .queryParam("kc_locale", "es")
                .build()
                .encode()
                .toUriString();
        return "redirect:" + cuentaKeycloak;
    }

    @GetMapping("/donante/mapa")
    public String mapaDonante(Authentication authentication, Model model) {
        model.addAttribute("nombreDonante", nombreDonante(authentication));
        model.addAttribute("entidades", donacionesClient.obtenerEntidades());
        cargarIncentivosDonante(authentication, model);
        List<DonacionDTO> historial = donacionesClient.obtenerTodasLasDonaciones().stream()
                .filter(donacion -> donacion.getDonanteName() != null
                        && donacion.getDonanteName().equalsIgnoreCase(authentication.getName()))
                .toList();
        model.addAttribute("historialDonaciones", historial);
        return "donante/mapa";
    }

    private void cargarIncentivosDonante(Authentication authentication, Model model) {
        List<PersonaDonanteDTO> coincidencias = donacionesClient.obtenerDonantes().stream()
                .filter(donante -> coincideConCuenta(donante, authentication))
                .toList();
        if (coincidencias.size() != 1 || coincidencias.get(0).getId() == null) {
            model.addAttribute("incentivosDisponibles", false);
            model.addAttribute("insignias", List.of());
            model.addAttribute("rankingTop3", List.of());
            return;
        }

        UUID idDonante = coincidencias.get(0).getId();
        Map<String, Object> mision = incentivosClient.obtenerMision(idDonante.toString());
        int progreso = numero(mision.get("progresoActual"));
        int objetivo = numero(mision.get("progresoObjetivo"));
        int porcentaje = objetivo > 0 ? Math.min(100, Math.max(0, progreso * 100 / objetivo)) : 0;
        Map<String, Object> rankingMensual = incentivosClient.obtenerRankingActual();
        Object posiciones = rankingMensual.get("ranking");
        List<Map<String, Object>> top3 = posiciones instanceof List<?> lista
                ? lista.stream().filter(Map.class::isInstance)
                    .map(posicion -> (Map<String, Object>) posicion).limit(3).toList()
                : List.of();

        model.addAttribute("misionDonante", mision);
        model.addAttribute("progresoMision", progreso);
        model.addAttribute("objetivoMision", objetivo);
        model.addAttribute("porcentajeMision", porcentaje);
        model.addAttribute("insignias", incentivosClient.obtenerInsignias(idDonante.toString()));
        model.addAttribute("puestoRanking", incentivosClient.obtenerPuestoRanking(idDonante.toString()));
        model.addAttribute("rankingTop3", top3);
        model.addAttribute("periodoRanking", rankingMensual.get("periodo"));
        model.addAttribute("incentivosDisponibles", !mision.isEmpty() || !rankingMensual.isEmpty());
    }

    private static boolean coincideConCuenta(PersonaDonanteDTO donante, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser usuario)) return false;
        String nombreCuenta = normalizar(usuario.getFullName());
        if (nombreCuenta.isBlank()) return false;
        String nombreDonante = "JURIDICA".equalsIgnoreCase(donante.getTipoPersona())
                ? donante.getRazonSocial()
                : (donante.getNombre() == null ? "" : donante.getNombre()) + " "
                    + (donante.getApellido() == null ? "" : donante.getApellido());
        return nombreCuenta.equals(normalizar(nombreDonante));
    }

    private static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private static int numero(Object valor) {
        return valor instanceof Number numero ? numero.intValue() : 0;
    }

    private static String nombreDonante(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String fullName = oidcUser.getFullName();
            if (fullName != null && !fullName.isBlank()) return fullName;
        }
        return authentication == null ? "Mi perfil" : authentication.getName();
    }

    @GetMapping("/chofer")
    public String chofer(Authentication authentication, Model model) {
        return perfil(authentication, model, "Espacio chofer", "CHOFER",
                "Desde acá vas a poder consultar tus rutas y actualizar el estado de tus entregas.");
    }

    @GetMapping("/entidad")
    public String entidad(Authentication authentication, Model model) {
        return perfil(authentication, model, "Espacio entidad beneficiaria", "ENTIDAD",
                "Desde acá vas a poder consultar y actualizar las necesidades de tu entidad.");
    }

    @GetMapping("/sin-acceso")
    public String sinAcceso() {
        return "sin-acceso";
    }

    private String perfil(Authentication authentication, Model model, String titulo, String rol, String descripcion) {
        model.addAttribute("titulo", titulo);
        model.addAttribute("rol", rol);
        model.addAttribute("descripcion", descripcion);
        model.addAttribute("usuario", authentication.getName());
        return "perfil";
    }
}
