package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ProfileController {
    private final DonacionesClient donacionesClient;
    private final String keycloakIssuerUri;
    private final String keycloakClientId;
    private final String keycloakAccountReturnUri;

    public ProfileController(DonacionesClient donacionesClient,
                             @Value("${keycloak.issuer-uri}") String keycloakIssuerUri,
                             @Value("${keycloak.client-id}") String keycloakClientId,
                             @Value("${keycloak.account-return-uri}") String keycloakAccountReturnUri) {
        this.donacionesClient = donacionesClient;
        this.keycloakIssuerUri = keycloakIssuerUri;
        this.keycloakClientId = keycloakClientId;
        this.keycloakAccountReturnUri = keycloakAccountReturnUri;
    }

    @GetMapping("/donante")
    public String donante(Authentication authentication, Model model) {
        model.addAttribute("nombreDonante", nombreDonante(authentication));
        return perfil(authentication, model, "Espacio donante", "DONANTE",
                "Desde acá vas a poder consultar tus donaciones y gestionar tu participación.");
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
        List<DonacionDTO> historial = donacionesClient.obtenerTodasLasDonaciones().stream()
                .filter(donacion -> donacion.getDonanteName() != null
                        && donacion.getDonanteName().equalsIgnoreCase(authentication.getName()))
                .toList();
        model.addAttribute("historialDonaciones", historial);
        return "donante/mapa";
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
