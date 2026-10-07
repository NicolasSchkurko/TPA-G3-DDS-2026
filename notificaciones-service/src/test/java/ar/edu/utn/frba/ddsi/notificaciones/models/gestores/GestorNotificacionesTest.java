package ar.edu.utn.frba.ddsi.notificaciones.models.gestores;

import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionPayload;
import ar.edu.utn.frba.ddsi.notificaciones.gateways.NotificacionGateway;
import ar.edu.utn.frba.ddsi.notificaciones.messaging.ProductorNotificaciones;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.Mail;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvioFactory;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.RepositorioNotificaciones;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El orden guardar → publicar y el uso de la dirección del mensaje. */
class GestorNotificacionesTest {

    private final RepositorioNotificaciones repositorio = mock(RepositorioNotificaciones.class);
    private final MedioDeEnvioFactory factory = mock(MedioDeEnvioFactory.class);
    private final ProductorNotificaciones productor = mock(ProductorNotificaciones.class);

    private final GestorNotificaciones gestor =
            new GestorNotificaciones(repositorio, factory, productor);

    @AfterEach
    void limpiarSincronizacion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void simularTransaccionActiva() {
        when(repositorio.save(any(Notificacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        TransactionSynchronizationManager.initSynchronization();
    }

    private void dispararAfterCommit() {
        for (TransactionSynchronization sincronizacion :
                TransactionSynchronizationManager.getSynchronizations()) {
            sincronizacion.afterCommit();
        }
    }

    @Test
    void noPublicaAntesDeQueLaTransaccionCommitee() {
        simularTransaccionActiva();

        gestor.enviarSolicitudDeNotificacion("email", "ana@test.com", "Asunto", "Cuerpo");

        verify(productor, never()).enviar(any(Notificacion.class));

        dispararAfterCommit();

        verify(productor, times(1)).enviar(any(Notificacion.class));
    }

    @Test
    void noPublicaSiLaTransaccionSeRevirtio() {
        simularTransaccionActiva();

        gestor.enviarSolicitudDeNotificacion("email", "ana@test.com", "Asunto", "Cuerpo");

        // Rollback: Spring limpia las sincronizaciones sin llamar afterCommit.
        TransactionSynchronizationManager.clearSynchronization();

        verify(productor, never()).enviar(any(Notificacion.class));
    }

    private String direccionUsadaAlEnviar(String direccionDelParametro, String direccionDeLaFila) {
        NotificacionGateway gateway = mock(NotificacionGateway.class);
        Mail mail = new Mail(gateway);
        MedioDeEnvioFactory factoryConMail = new MedioDeEnvioFactory(Map.of("email", mail));
        GestorNotificaciones gestorReal = new GestorNotificaciones(
                mock(RepositorioNotificaciones.class), factoryConMail, mock(ProductorNotificaciones.class));

        Notificacion notificacion =
                new Notificacion(direccionDeLaFila, "email", new Mensaje("Asunto", "Cuerpo"));

        gestorReal.enviarNotificacion("email", direccionDelParametro, notificacion);

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
}
