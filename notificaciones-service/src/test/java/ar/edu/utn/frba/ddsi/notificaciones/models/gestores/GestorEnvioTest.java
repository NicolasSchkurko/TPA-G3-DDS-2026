package ar.edu.utn.frba.ddsi.notificaciones.models.gestores;

import ar.edu.utn.frba.ddsi.notificaciones.dto.n8n.NotificacionPayload;
import ar.edu.utn.frba.ddsi.notificaciones.gateways.NotificacionGateway;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.Mail;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvioFactory;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** El gestor de envío elige el medio y no envuelve las fallas de red. */
class GestorEnvioTest {

    private String direccionUsadaAlEnviar(String direccionDelParametro, String direccionDeLaFila) {
        NotificacionGateway gateway = mock(NotificacionGateway.class);
        Mail mail = new Mail(gateway);
        MedioDeEnvioFactory factoryConMail = new MedioDeEnvioFactory(Map.of("email", mail));
        GestorEnvio gestor = new GestorEnvio(factoryConMail);

        Notificacion notificacion =
                new Notificacion(direccionDeLaFila, "email", new Mensaje("Asunto", "Cuerpo"));

        gestor.enviarNotificacion("email", direccionDelParametro, notificacion);

        ArgumentCaptor<NotificacionPayload> captor = ArgumentCaptor.forClass(NotificacionPayload.class);
        verify(gateway).enviar(captor.capture());
        return captor.getValue().getDireccionContacto();
    }

    @Test
    void usaLaDireccionDelMensajeCuandoViene() {
        assertEquals("nueva@test.com",
                direccionUsadaAlEnviar("nueva@test.com", "vieja@test.com"));
    }

    @Test
    void siElMensajeNoTraeDireccionUsaLaDeLaFila() {
        assertEquals("vieja@test.com", direccionUsadaAlEnviar(null, "vieja@test.com"));
        assertEquals("vieja@test.com", direccionUsadaAlEnviar("   ", "vieja@test.com"));
    }

    @Test
    void reclamarPasaLaFilaAEnviandoYPersisteElDestinoDelAviso() {
        GestorEnvio gestor = new GestorEnvio(mock(MedioDeEnvioFactory.class));
        Notificacion notificacion = new Notificacion("vieja@test.com", "email",
                new Mensaje("A", "C"));

        Optional<Notificacion> reclamada = gestor.reclamar(
                Optional.of(notificacion), "whatsapp", "nueva@test.com");

        assertTrue(reclamada.isPresent());
        assertEquals(EstadoNotificacion.ENVIANDO, notificacion.getEstado());
        assertNotNull(notificacion.getFechaIntento());
        assertNotNull(notificacion.getClaimId());
        assertEquals("whatsapp", notificacion.getTipoMedioDeContacto());
        assertEquals("nueva@test.com", notificacion.getDireccionDeContacto());
    }

    @Test
    void reclamarNoRepiteUnaEnviadaNiUnaEnCurso() {
        GestorEnvio gestor = new GestorEnvio(mock(MedioDeEnvioFactory.class));
        assertTrue(gestor.reclamar(Optional.empty(), "email", "ana@test.com").isEmpty());

        Notificacion enviada = new Notificacion("ana@test.com", "email",
                new Mensaje("A", "C"));
        enviada.marcarEnviada();
        assertTrue(gestor.reclamar(Optional.of(enviada), "email", "ana@test.com").isEmpty());

        Notificacion enCurso = new Notificacion("ana@test.com", "email",
                new Mensaje("A", "C"));
        enCurso.marcarEnviando(LocalDateTime.now());
        assertTrue(gestor.reclamar(Optional.of(enCurso), "email", "ana@test.com").isEmpty());
    }

    @Test
    void propagaLaFallaDeRedSinEnvolverlaEnIllegalArgumentException() {
        NotificacionGateway gateway = mock(NotificacionGateway.class);
        doThrow(new ResourceAccessException("timeout contra n8n"))
                .when(gateway).enviar(any());
        Mail mail = new Mail(gateway);
        MedioDeEnvioFactory factoryConMail = new MedioDeEnvioFactory(Map.of("email", mail));
        GestorEnvio gestor = new GestorEnvio(factoryConMail);

        Notificacion notificacion =
                new Notificacion("ana@test.com", "email", new Mensaje("Asunto", "Cuerpo"));

        assertThrows(ResourceAccessException.class,
                () -> gestor.enviarNotificacion("email", null, notificacion));
        assertEquals(EstadoNotificacion.PENDIENTE, notificacion.getEstado());
    }
}
