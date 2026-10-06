package ar.edu.utn.frba.ddsi.notificaciones.test_integracion;

import ar.edu.utn.frba.ddsi.notificaciones.gateways.NotificacionGateway;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.Mail;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
public class N8nIntegrationTest {

    @Autowired
    private NotificacionGateway gateway;

    @Test
    void deberiaEnviarMailRealAN8n() throws Exception {

        Mail mail = new Mail(gateway);

        Notificacion n = new Notificacion(
                "merotero@frba.utn.edu.ar",
                "WHATSAPP",
                new Mensaje(
                        "Test de integración con n8n",
                        "Mensaje..."
                )
        );

        mail.enviarNotificacion(n);
    }
}
