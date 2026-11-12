package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.KeycloakRegistrationClient;
import ar.edu.utn.frba.ddsi.front.services.VerificacionEmailTokenService;
import ar.edu.utn.frba.ddsi.front.services.VerificacionEmailService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    private final VerificacionEmailService verificacionEmailService;

    public VerificacionController(VerificacionEmailTokenService tokenService,
                                  KeycloakRegistrationClient keycloakRegistrationClient,
                                  VerificacionEmailService verificacionEmailService) {
        this.tokenService = tokenService;
        this.keycloakRegistrationClient = keycloakRegistrationClient;
        this.verificacionEmailService = verificacionEmailService;
    }

    @GetMapping("/verificar")
    public String verificar(@RequestParam(required = false) String token,
                            @AuthenticationPrincipal OidcUser usuario,
                            HttpSession session,
                            Model model) {
        Optional<String> userId = tokenService.resolver(token);
        if (userId.isEmpty()) {
            model.addAttribute("estado", "invalido");
            return "verificacion";
        }
        try {
            keycloakRegistrationClient.marcarEmailVerificado(userId.get());
            model.addAttribute("estado", "ok");
            // Si quien abre el link es la misma persona logueada, ocultamos el aviso desde ya.
            if (usuario != null && userId.get().equals(usuario.getSubject())) {
                session.setAttribute("emailVerificadoEnSesion", true);
                model.addAttribute("emailSinVerificar", false);
            }
        } catch (Exception e) {
            log.warn("No se pudo marcar el email como verificado para {}: {}", userId.get(), e.getMessage());
            model.addAttribute("estado", "error");
        }
        return "verificacion";
    }

    /** Reenvía el mail de verificación a la persona logueada (como mucho uno por minuto). */
    @PostMapping("/verificar/reenviar")
    public String reenviar(@AuthenticationPrincipal OidcUser usuario,
                           HttpSession session,
                           @RequestHeader(value = "Referer", required = false) String referer,
                           RedirectAttributes flash) {
        Object ultimoEnvio = session.getAttribute("verificacionUltimoEnvio");
        long ahora = System.currentTimeMillis();
        if (ultimoEnvio instanceof Long ultimo && ahora - ultimo < 60_000) {
            flash.addFlashAttribute("verificacionMensaje",
                    "Ya te enviamos un correo hace menos de un minuto. Revisá tu bandeja y la carpeta de spam.");
        } else if (verificacionEmailService.enviar(usuario.getSubject(), usuario.getEmail())) {
            session.setAttribute("verificacionUltimoEnvio", ahora);
            flash.addFlashAttribute("verificacionMensaje",
                    "Te enviamos un nuevo correo a " + usuario.getEmail() + ". El link vence en 24 horas.");
        } else {
            flash.addFlashAttribute("verificacionMensaje",
                    "No pudimos enviar el correo. Intentá de nuevo en unos minutos.");
        }
        return "redirect:" + (referer != null && referer.startsWith("http://localhost:8080") ? referer : "/");
    }
}
