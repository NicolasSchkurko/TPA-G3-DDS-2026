package ar.edu.utn.frba.ddsi.notificaciones.mappers;

import ar.edu.utn.frba.ddsi.notificaciones.dto.Notificaciones.NotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** El mapeo de {@code Notificacion} al {@link NotificacionDTO} que expone el GET. */
class NotificacionMapperTest {

    private final NotificacionMapper mapper = new NotificacionMapper();

    @Test
    void mapeaTodosLosCamposIncluidoElTipoDeMedioDeContacto() {
        Notificacion notificacion = new Notificacion(
                "ana@test.com", "email", new Mensaje("Asunto", "Cuerpo"));

        NotificacionDTO dto = mapper.notificacionDTO(notificacion);

        assertEquals(notificacion.getId().toString(), dto.getId(), "el GET debe devolver el id del registro");
        assertEquals("Asunto", dto.getAsunto());
        assertEquals("Cuerpo", dto.getCuerpo());
        assertEquals("ana@test.com", dto.getDireccionDeContacto());
        assertEquals("email", dto.getTipoMedioDeContacto(), "era el campo que se perdía");
        assertEquals("PENDIENTE", dto.getEstado());
        assertNotNull(dto.getFechaCreacion());
        assertNull(dto.getFechaEnvio());
    }

    @Test
    void laFechaDeEnvioSeMapeaCuandoLaNotificacionYaSeEnvio() {
        Notificacion notificacion = new Notificacion(
                "ana@test.com", "email", new Mensaje("Asunto", "Cuerpo"));
        notificacion.marcarEnviada();

        NotificacionDTO dto = mapper.notificacionDTO(notificacion);

        assertEquals("ENVIADA", dto.getEstado());
        assertNotNull(dto.getFechaEnvio());
    }

    @Test
    void mapeaElUrlMapaCuandoElMensajeLoTiene() {
        Notificacion notificacion = new Notificacion("ana@test.com", "email",
                new Mensaje("Asunto", "Cuerpo", "https://mapa/1"));

        NotificacionDTO dto = mapper.notificacionDTO(notificacion);

        assertEquals("https://mapa/1", dto.getUrlMapa());
    }

    @Test
    void noFallaSiLaNotificacionNoTieneMensaje() {
        Notificacion notificacion = new Notificacion();
        notificacion.setEstado(EstadoNotificacion.PENDIENTE);

        NotificacionDTO dto = assertDoesNotThrow(() -> mapper.notificacionDTO(notificacion));

        assertNull(dto.getAsunto());
        assertNull(dto.getCuerpo());
        assertEquals("PENDIENTE", dto.getEstado());
    }

    @Test
    void noFallaSiEstadoOFechaCreacionSonNulos() {
        Notificacion notificacion = new Notificacion();
        notificacion.setMensaje(new Mensaje("Asunto", "Cuerpo"));
        notificacion.setDireccionDeContacto("ana@test.com");

        NotificacionDTO dto = assertDoesNotThrow(() -> mapper.notificacionDTO(notificacion));

        assertEquals("Asunto", dto.getAsunto());
        assertNull(dto.getEstado());
        assertNull(dto.getFechaCreacion());
    }
}
