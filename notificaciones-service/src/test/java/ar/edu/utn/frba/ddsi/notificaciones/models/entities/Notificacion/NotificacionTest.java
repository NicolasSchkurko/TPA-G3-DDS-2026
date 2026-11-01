package ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificacionTest {

    @Test
    @DisplayName("Notificación recién creada nace en estado PENDIENTE con fecha de creación y sin fecha de envío")
    void notificacionRecienCreadaNacePendiente() {
        Mensaje mensaje = new Mensaje("Asunto", "Cuerpo");
        Notificacion notificacion = new Notificacion("id-sol-1", "ana@test.com", "email", mensaje);

        assertNotNull(notificacion.getId());
        assertEquals("id-sol-1", notificacion.getIdSolicitud());
        assertEquals(EstadoNotificacion.PENDIENTE, notificacion.getEstado());
        assertNotNull(notificacion.getFechaCreacion());
        assertNull(notificacion.getFechaEnvio());
        assertEquals("ana@test.com", notificacion.getDireccionDeContacto());
        assertEquals("email", notificacion.getTipoMedioDeContacto());
        assertEquals(mensaje, notificacion.getMensaje());
    }

    @Test
    @DisplayName("marcarEnviada() transiciona el estado a ENVIADA y fija la fecha de envío")
    void marcarEnviadaFijaEstadoYFechaEnvio() {
        Notificacion notificacion = new Notificacion("ana@test.com", "email", new Mensaje("A", "C"));
        LocalDateTime antesDeEnviar = LocalDateTime.now().minusNanos(1000);

        notificacion.marcarEnviada();

        assertEquals(EstadoNotificacion.ENVIADA, notificacion.getEstado());
        assertNotNull(notificacion.getFechaEnvio());
        assertTrue(notificacion.getFechaEnvio().isAfter(antesDeEnviar) || notificacion.getFechaEnvio().isEqual(antesDeEnviar));
    }

    @Test
    @DisplayName("marcarFallida() transiciona el estado a FALLIDA y no modifica la fecha de envío")
    void marcarFallidaFijaEstadoSinFechaEnvio() {
        Notificacion notificacion = new Notificacion("ana@test.com", "email", new Mensaje("A", "C"));

        notificacion.marcarFallida();

        assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstado());
        assertNull(notificacion.getFechaEnvio());
    }
}
