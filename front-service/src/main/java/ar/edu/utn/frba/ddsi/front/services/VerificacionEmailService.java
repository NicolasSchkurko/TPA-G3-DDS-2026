package ar.edu.utn.frba.ddsi.front.services;

import ar.edu.utn.frba.ddsi.front.clients.NotificacionesServicioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Arma y envía el mail con el link de verificación de la cuenta.
 */
@Service
public class VerificacionEmailService {

    private final VerificacionEmailTokenService tokenService;
    private final NotificacionesServicioClient notificacionesClient;
    private final String frontBaseUrl;

    public VerificacionEmailService(VerificacionEmailTokenService tokenService,
                                    NotificacionesServicioClient notificacionesClient,
                                    @Value("${front.base-url:http://localhost:8080}") String frontBaseUrl) {
        this.tokenService = tokenService;
        this.notificacionesClient = notificacionesClient;
        this.frontBaseUrl = frontBaseUrl.endsWith("/")
                ? frontBaseUrl.substring(0, frontBaseUrl.length() - 1)
                : frontBaseUrl;
    }

    /** Link que va en el mail. */
    public String linkDeVerificacion(String userId) {
        return frontBaseUrl + "/verificar?token=" + tokenService.generar(userId);
    }

    /** Envía el mail de verificación. Devuelve true si notificaciones aceptó el pedido. */
    public boolean enviar(String userId, String email) {
        if (userId == null || email == null) return false;
        String link = linkDeVerificacion(userId);
        String asunto = "DonaTrack · Verificá tu correo electrónico";
        String cuerpo = """
                ¡Hola! Recibimos un pedido para crear o vincular una cuenta de DonaTrack con este correo.

                Para confirmar que el correo es tuyo, abrí este link (vence en 24 horas):
                %s

                Si no fuiste vos, ignorá este mensaje: no se va a vincular ninguna cuenta.
                """.formatted(link);
        return notificacionesClient.enviarEmail(email, asunto, cuerpo);
    }
}
