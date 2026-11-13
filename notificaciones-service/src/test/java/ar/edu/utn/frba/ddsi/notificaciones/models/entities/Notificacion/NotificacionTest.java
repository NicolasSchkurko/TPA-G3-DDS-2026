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
    @DisplayName("crear() arma la solicitud con su mensaje y metadatos")
    void crearArmaLaSolicitudConSuMensaje() {
        Notificacion notificacion = Notificacion.crear(
                "sol-1", "email", "ana@test.com", "Asunto", "Cuerpo", "https://mapa/1");

        assertEquals("sol-1", notificacion.getIdSolicitud());
        assertEquals("email", notificacion.getTipoMedioDeContacto());
        assertEquals("ana@test.com", notificacion.getDireccionDeContacto());
        assertEquals("Asunto", notificacion.getMensaje().getAsunto());
        assertEquals("Cuerpo", notificacion.getMensaje().getCuerpo());
        assertEquals("https://mapa/1", notificacion.getMensaje().getUrlMapa());
        assertEquals(EstadoNotificacion.PENDIENTE, notificacion.getEstado());
    }

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
        assertNull(notificacion.getFechaIntento());
        assertEquals("ana@test.com", notificacion.getDireccionDeContacto());
        assertEquals("email", notificacion.getTipoMedioDeContacto());
        assertEquals(mensaje, notificacion.getMensaje());
    }

    @Test
    @DisplayName("marcarEnviando() transiciona el estado a ENVIANDO y fija la fecha de intento")
    void marcarEnviandoFijaEstadoYFechaIntento() {
        Notificacion notificacion = new Notificacion("ana@test.com", "email", new Mensaje("A", "C"));
        LocalDateTime antes = LocalDateTime.now().minusNanos(1000);

        notificacion.marcarEnviando(LocalDateTime.now());

        assertEquals(EstadoNotificacion.ENVIANDO, notificacion.getEstado());
        assertNotNull(notificacion.getFechaIntento());
        assertNotNull(notificacion.getClaimId());
        assertTrue(notificacion.getFechaIntento().isAfter(antes)
                || notificacion.getFechaIntento().isEqual(antes));
    }

    @Test
    @DisplayName("marcarEnviada() transiciona el estado a ENVIADA, fija la fecha de envío y limpia el intento")
    void marcarEnviadaFijaEstadoYFechaEnvio() {
        Notificacion notificacion = new Notificacion("ana@test.com", "email", new Mensaje("A", "C"));
        notificacion.marcarEnviando(LocalDateTime.now().minusSeconds(5));
        LocalDateTime antesDeEnviar = LocalDateTime.now().minusNanos(1000);

        notificacion.marcarEnviada();

        assertEquals(EstadoNotificacion.ENVIADA, notificacion.getEstado());
        assertNotNull(notificacion.getFechaEnvio());
        assertNull(notificacion.getFechaIntento());
        assertNull(notificacion.getClaimId());
        assertTrue(notificacion.getFechaEnvio().isAfter(antesDeEnviar) || notificacion.getFechaEnvio().isEqual(antesDeEnviar));
    }

    @Test
    @DisplayName("marcarFallida() transiciona el estado a FALLIDA, no fija fecha de envío y limpia el intento")
    void marcarFallidaFijaEstadoSinFechaEnvio() {
        Notificacion notificacion = new Notificacion("ana@test.com", "email", new Mensaje("A", "C"));
        notificacion.marcarEnviando(LocalDateTime.now().minusSeconds(5));

        notificacion.marcarFallida();

        assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstado());
        assertNull(notificacion.getFechaEnvio());
        assertNull(notificacion.getFechaIntento());
        assertNull(notificacion.getClaimId());
    }
}
