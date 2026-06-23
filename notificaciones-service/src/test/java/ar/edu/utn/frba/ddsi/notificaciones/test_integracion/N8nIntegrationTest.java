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
                "INSERTAR_MAIL_PARA_TESTEAR",
                new Mensaje("Test de integracion con n8n", "Mensaje que ves si la prueba funciono")
        );

        mail.enviarNotificacion(n);
    }
}
