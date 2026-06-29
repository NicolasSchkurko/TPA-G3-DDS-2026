package ar.edu.utn.frba.ddsi.notificaciones;

import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionPayload;
import ar.edu.utn.frba.ddsi.notificaciones.gateways.NotificacionGateway;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.Mail;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;



public class NotificacionesServiceApplicationTests {


    @Test
    void debeEnviarNotificacionPorMail() throws Exception {
        NotificacionGateway gatewayMock = mock(NotificacionGateway.class);

        Mail mail = new Mail(gatewayMock);

        Mensaje mensaje = new Mensaje("Asunto test", "Cuerpo test");
        Notificacion notificacion = new Notificacion("test@mail.com", mensaje);

        mail.enviarNotificacion(notificacion);

        verify(gatewayMock, times(1)).enviar(any(NotificacionPayload.class));
    }

    @Test
    void debeConstruirCorrectamenteElPayload() throws Exception {
        NotificacionGateway gatewayMock = mock(NotificacionGateway.class);

        Mail mail = new Mail(gatewayMock);

        Mensaje mensaje = new Mensaje("Asunto test", "Cuerpo test");
        Notificacion notificacion = new Notificacion("test@mail.com", mensaje);

        mail.enviarNotificacion(notificacion);

        ArgumentCaptor<NotificacionPayload> captor =
                ArgumentCaptor.forClass(NotificacionPayload.class);

        verify(gatewayMock).enviar(captor.capture());

        NotificacionPayload payload = captor.getValue();


        assertEquals("email", payload.getCanal());
        assertEquals("test@mail.com", payload.getDireccionContacto());
        assertEquals("Asunto test", payload.getMensaje().getAsunto());
        assertEquals("Cuerpo test", payload.getMensaje().getCuerpo());
    }


}
