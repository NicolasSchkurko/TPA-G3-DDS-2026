package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.KeycloakRegistrationClient;
import ar.edu.utn.frba.ddsi.front.services.VerificacionEmailTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * Recibe el link del mail de verificación. Si el token es válido y no venció,
 * marca el email de la cuenta de Keycloak como verificado.
 */
@Controller
public class VerificacionController {

    private static final Logger log = LoggerFactory.getLogger(VerificacionController.class);

    private final VerificacionEmailTokenService tokenService;
    private final KeycloakRegistrationClient keycloakRegistrationClient;

    public VerificacionController(VerificacionEmailTokenService tokenService,
                                  KeycloakRegistrationClient keycloakRegistrationClient) {
        this.tokenService = tokenService;
        this.keycloakRegistrationClient = keycloakRegistrationClient;
    }

    @GetMapping("/verificar")
    public String verificar(@RequestParam(required = false) String token, Model model) {
        Optional<String> userId = tokenService.resolver(token);
        if (userId.isEmpty()) {
            model.addAttribute("estado", "invalido");
            return "verificacion";
        }
        try {
            keycloakRegistrationClient.marcarEmailVerificado(userId.get());
            model.addAttribute("estado", "ok");
        } catch (Exception e) {
            log.warn("No se pudo marcar el email como verificado para {}: {}", userId.get(), e.getMessage());
            model.addAttribute("estado", "error");
        }
        return "verificacion";
    }
}
