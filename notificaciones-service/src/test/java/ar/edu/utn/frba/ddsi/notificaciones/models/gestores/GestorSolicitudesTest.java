package ar.edu.utn.frba.ddsi.notificaciones.models.gestores;

import ar.edu.utn.frba.ddsi.notificaciones.dto.Notificaciones.SolicitudNotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionNoReintentableException;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reglas de dominio de la solicitud: campos obligatorios y largo del urlMapa. */
class GestorSolicitudesTest {

    private final GestorSolicitudes gestor = new GestorSolicitudes();

    private SolicitudNotificacionDTO solicitud() {
        SolicitudNotificacionDTO dto = new SolicitudNotificacionDTO();
        dto.setMedioDeContacto("email");
        dto.setDireccionDeContacto("ana@test.com");
        dto.setAsuntoMensaje("Asunto");
        dto.setCuerpoMensaje("Cuerpo");
        return dto;
    }

    @Test
    void aceptaUnaSolicitudCompleta() {
        assertDoesNotThrow(() -> gestor.validar(solicitud()));
    }

    @Test
    void rechazaLosCamposObligatoriosVacios() {
        SolicitudNotificacionDTO sinMedio = solicitud();
        sinMedio.setMedioDeContacto(" ");
        assertThrows(IllegalArgumentException.class, () -> gestor.validar(sinMedio));

        SolicitudNotificacionDTO sinDestino = solicitud();
        sinDestino.setDireccionDeContacto(null);
        assertThrows(IllegalArgumentException.class, () -> gestor.validar(sinDestino));

        SolicitudNotificacionDTO sinAsunto = solicitud();
        sinAsunto.setAsuntoMensaje("");
        assertThrows(IllegalArgumentException.class, () -> gestor.validar(sinAsunto));

        SolicitudNotificacionDTO sinCuerpo = solicitud();
        sinCuerpo.setCuerpoMensaje(null);
        assertThrows(IllegalArgumentException.class, () -> gestor.validar(sinCuerpo));
    }

    @Test
    void rechazaUnUrlMapaDemasiadoLargo() {
        SolicitudNotificacionDTO dto = solicitud();
        dto.setUrlMapa("a".repeat(1025));

        assertThrows(IllegalArgumentException.class, () -> gestor.validar(dto));
    }

    @Test
    void idSiYaExisteDevuelveElIdSoloSiHayNotificacion() {
        assertTrue(gestor.idSiYaExiste(Optional.empty()).isEmpty());

        Notificacion notificacion = new Notificacion("ana@test.com", "email",
                new Mensaje("Asunto", "Cuerpo"));
        assertEquals(notificacion.getId(),
                gestor.idSiYaExiste(Optional.of(notificacion)).orElseThrow());
    }

    @Test
    void validarReintentoRechazaEnviadaYEnviando() {
        assertDoesNotThrow(() -> gestor.validarReintento(
                new Notificacion("ana@test.com", "email", new Mensaje("A", "C"))));

        Notificacion enviada = new Notificacion("ana@test.com", "email", new Mensaje("A", "C"));
        enviada.marcarEnviada();
        assertThrows(NotificacionNoReintentableException.class,
                () -> gestor.validarReintento(enviada));

        Notificacion enCurso = new Notificacion("ana@test.com", "email", new Mensaje("A", "C"));
        enCurso.marcarEnviando(LocalDateTime.now());
        assertThrows(NotificacionNoReintentableException.class,
                () -> gestor.validarReintento(enCurso));
    }
}
