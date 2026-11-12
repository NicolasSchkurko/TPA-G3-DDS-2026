package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.services.IdentidadService;
import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class ProfileController {
    private final DonacionesClient donacionesClient;
    private final IncentivosClient incentivosClient;
    private final IdentidadService identidadService;
    private final String keycloakIssuerUri;
    private final String keycloakClientId;
    private final String keycloakAccountReturnUri;

    public ProfileController(DonacionesClient donacionesClient,
                             IncentivosClient incentivosClient,
                             IdentidadService identidadService,
                             @Value("${keycloak.issuer-uri}") String keycloakIssuerUri,
                             @Value("${keycloak.client-id}") String keycloakClientId,
                             @Value("${keycloak.account-return-uri}") String keycloakAccountReturnUri) {
        this.donacionesClient = donacionesClient;
        this.incentivosClient = incentivosClient;
        this.identidadService = identidadService;
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
        UUID idDonante = identidadService.donanteIdActual(authentication).orElse(null);
        cargarIncentivosDonante(idDonante, model);
        List<DonacionDTO> historial = idDonante == null
                ? List.of()
                : donacionesClient.obtenerDonacionesDeDonante(idDonante);
        model.addAttribute("historialDonaciones", historial);
        return "donante/mapa";
    }

    private void cargarIncentivosDonante(UUID idDonante, Model model) {
        if (idDonante == null) {
            model.addAttribute("incentivosDisponibles", false);
            model.addAttribute("insignias", List.of());
            model.addAttribute("rankingTop3", List.of());
            return;
        }
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

    @GetMapping("/sin-acceso")
    public String sinAcceso() {
        return "sin-acceso";
    }
}
