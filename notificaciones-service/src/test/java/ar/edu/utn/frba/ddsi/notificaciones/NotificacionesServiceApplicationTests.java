package ar.edu.utn.frba.ddsi.notificaciones;

import ar.edu.utn.frba.ddsi.notificaciones.config.mailClient.MailClient;
import ar.edu.utn.frba.ddsi.notificaciones.config.telefonoClient.TelefonoClient;
import ar.edu.utn.frba.ddsi.notificaciones.config.whatsappClient.WhatsappClient;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.Mail;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.Telefono;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.Whatsapp;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class NotificacionesServiceApplicationTests {

    private final WhatsappClient whatsappClient = mock(WhatsappClient.class);
    private final TelefonoClient telefonoClient = mock(TelefonoClient.class);
    private final MailClient mailClient = mock(MailClient.class);

    @Test
    @DisplayName("Se envia notificacion por Whatsapp usando el WhatsappClient")
    void whatsappEnviaNotificacionUsandoClienteWhatsapp() {
        Notificacion notificacion = crearNotificacion();

        new Whatsapp(whatsappClient).enviarNotificacion("5491112345678", notificacion);

        verify(whatsappClient).enviar("5491112345678", notificacion);
    }

    @Test
    @DisplayName("Se envia notificacion por Telefono usando el TelefonoClient")
    void telefonoEnviaNotificacionUsandoClienteTelefono() {
        Notificacion notificacion = crearNotificacion();

        new Telefono(telefonoClient).enviarNotificacion("5491112345678", notificacion);

        verify(telefonoClient).enviar("5491112345678", notificacion);
    }

    @Test
    @DisplayName("Se envia notificacion por Mail usando el MailClient")
    void mailEnviaNotificacionUsandoClienteMail() {
        Notificacion notificacion = crearNotificacion();

        new Mail(mailClient).enviarNotificacion("persona@mail.com", notificacion);

        verify(mailClient).enviar("persona@mail.com", notificacion);
    }

    private Notificacion crearNotificacion() {
        return new Notificacion(
                "persona@mail.com",
                new Mensaje("Asunto de prueba", "Cuerpo de prueba")
        );
    }
}
