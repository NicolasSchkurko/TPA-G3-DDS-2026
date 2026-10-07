package ar.edu.utn.frba.ddsi.notificaciones.mappers;

import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.junit.jupiter.api.Test;

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
}
