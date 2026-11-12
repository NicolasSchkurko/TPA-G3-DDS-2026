package ar.edu.utn.frba.ddsi.notificaciones.services;

import ar.edu.utn.frba.ddsi.notificaciones.dto.Notificaciones.NotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.dto.Notificaciones.SolicitudNotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionNoReintentableException;
import ar.edu.utn.frba.ddsi.notificaciones.mappers.NotificacionMapper;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.messaging.ProductorNotificaciones;
import ar.edu.utn.frba.ddsi.notificaciones.models.gestores.GestorEnvio;
import ar.edu.utn.frba.ddsi.notificaciones.models.gestores.GestorSolicitudes;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories.RepositorioNotificaciones;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificadorServiceTest {

    private final RepositorioNotificaciones repositorio = mock(RepositorioNotificaciones.class);
    private final GestorSolicitudes gestorSolicitudes = new GestorSolicitudes();
    private final GestorEnvio gestorEnvio = mock(GestorEnvio.class);
    private final ProductorNotificaciones productor = mock(ProductorNotificaciones.class);
    private final NotificadorService service = new NotificadorService(
            repositorio, gestorSolicitudes, gestorEnvio, productor, new NotificacionMapper());

    @AfterEach
    void limpiarSincronizacion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private SolicitudNotificacionDTO solicitud(String idSolicitud) {
        SolicitudNotificacionDTO dto = new SolicitudNotificacionDTO();
        dto.setIdSolicitud(idSolicitud);
        dto.setMedioDeContacto("email");
        dto.setDireccionDeContacto("ana@test.com");
        dto.setAsuntoMensaje("Asunto");
        dto.setCuerpoMensaje("Cuerpo");
        return dto;
    }

    private Notificacion notificacion() {
        return new Notificacion("ana@test.com", "email", new Mensaje("Asunto", "Cuerpo"));
    }

    @Test
    void procesarSolicitudGuardaYPublica() {
        when(repositorio.save(any(Notificacion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        UUID id = service.procesarSolicitudDeNotificacion(solicitud("sol-1"));

        assertNotNull(id);
        verify(repositorio).save(any(Notificacion.class));
        verify(productor).enviar(any(Notificacion.class));
    }

    @Test
    void noPublicaAntesDelCommit() {
        when(repositorio.save(any(Notificacion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        TransactionSynchronizationManager.initSynchronization();

        service.procesarSolicitudDeNotificacion(solicitud("sol-commit"));

        verify(productor, never()).enviar(any(Notificacion.class));
        for (TransactionSynchronization sincronizacion :
                TransactionSynchronizationManager.getSynchronizations()) {
            sincronizacion.afterCommit();
        }
        verify(productor).enviar(any(Notificacion.class));
    }

    @Test
    void procesarSolicitudIdempotenteNoGuardaNiPublica() {
        Notificacion existente = notificacion();
        when(repositorio.findByIdSolicitud("sol-1")).thenReturn(Optional.of(existente));

        UUID id = service.procesarSolicitudDeNotificacion(solicitud("sol-1"));

        assertEquals(existente.getId(), id);
        verify(repositorio, never()).save(any(Notificacion.class));
        verify(productor, never()).enviar(any(Notificacion.class));
    }

    @Test
    void listaPaginadoYFiltraPorEstado() {
        Notificacion notificacion = notificacion();
        Pageable pageable = PageRequest.of(0, 5);
        when(repositorio.buscar(null, pageable))
                .thenReturn(new PageImpl<>(List.of(notificacion), pageable, 1));
        when(repositorio.buscar(EstadoNotificacion.FALLIDA, pageable))
                .thenReturn(new PageImpl<>(List.of(notificacion), pageable, 1));

        Page<NotificacionDTO> todas = service.listar(null, pageable);
        Page<NotificacionDTO> fallidas = service.listar(EstadoNotificacion.FALLIDA, pageable);

        assertEquals(notificacion.getId().toString(), todas.getContent().get(0).getId());
        assertEquals(1, fallidas.getTotalElements());
        verify(repositorio).buscar(null, pageable);
        verify(repositorio).buscar(EstadoNotificacion.FALLIDA, pageable);
    }

    @Test
    void obtienePorId() {
        UUID id = UUID.randomUUID();
        Notificacion notificacion = notificacion();
        when(repositorio.findById(id)).thenReturn(Optional.of(notificacion));

        assertTrue(service.obtenerPorId(id).isPresent());
        verify(repositorio).findById(id);
    }

    @Test
    void reintentarUnaNotificacionInexistenteDevuelve404YNoRepublica() {
        UUID id = UUID.randomUUID();
        when(repositorio.findById(id)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.reintentar(id));

        assertEquals(404, error.getStatusCode().value());
        verify(productor, never()).enviar(any());
    }

    @Test
    void reintentarUnaNotificacionYaEnviadaDevuelve409YNoRepublica() {
        UUID id = UUID.randomUUID();
        Notificacion enviada = notificacion();
        enviada.marcarEnviada();
        when(repositorio.findById(id)).thenReturn(Optional.of(enviada));

        assertThrows(NotificacionNoReintentableException.class,
                () -> service.reintentar(id));
        verify(productor, never()).enviar(any());
    }

    @Test
    void reintentarUnaNotificacionEnviandoDevuelve409YNoRepublica() {
        UUID id = UUID.randomUUID();
        Notificacion enCurso = notificacion();
        enCurso.marcarEnviando(LocalDateTime.now());
        when(repositorio.findById(id)).thenReturn(Optional.of(enCurso));

        assertThrows(NotificacionNoReintentableException.class,
                () -> service.reintentar(id));
        verify(productor, never()).enviar(any());
    }

    @Test
    void reintentarUnaNotificacionFallidaLaRepublica() {
        UUID id = UUID.randomUUID();
        Notificacion fallida = notificacion();
        fallida.marcarFallida();
        when(repositorio.findById(id)).thenReturn(Optional.of(fallida));

        service.reintentar(id);

        verify(productor).enviar(fallida);
    }

    @Test
    void reclamarParaEnvioDelegaEnElGestorDeEnvio() {
        Notificacion notificacion = notificacion();
        Optional<Notificacion> encontrada = Optional.of(notificacion);
        when(repositorio.findByIdParaEnvio(notificacion.getId())).thenReturn(encontrada);
        when(gestorEnvio.reclamar(encontrada, "whatsapp", "nueva@test.com"))
                .thenReturn(encontrada);

        Optional<Notificacion> reclamada = service.reclamarParaEnvio(
                notificacion.getId(), "whatsapp", "nueva@test.com");

        assertEquals(encontrada, reclamada);
        verify(gestorEnvio).reclamar(encontrada, "whatsapp", "nueva@test.com");
    }

    @Test
    void losCierresSonCondicionalesSobreElClaim() {
        UUID id = UUID.randomUUID();
        UUID claimId = UUID.randomUUID();
        when(repositorio.marcarEnviadaSiEnviando(eq(id), eq(claimId), any(LocalDateTime.class),
                eq(EstadoNotificacion.ENVIADA), eq(EstadoNotificacion.ENVIANDO))).thenReturn(1);
        when(repositorio.marcarFallidaSiEnviando(eq(id), eq(claimId),
                eq(EstadoNotificacion.FALLIDA), eq(EstadoNotificacion.ENVIANDO))).thenReturn(1);

        assertTrue(service.marcarEnviada(id, claimId));
        assertTrue(service.marcarFallida(id, claimId));
        verify(repositorio).marcarEnviadaSiEnviando(eq(id), eq(claimId),
                any(LocalDateTime.class), eq(EstadoNotificacion.ENVIADA), eq(EstadoNotificacion.ENVIANDO));
        verify(repositorio).marcarFallidaSiEnviando(eq(id), eq(claimId),
                eq(EstadoNotificacion.FALLIDA), eq(EstadoNotificacion.ENVIANDO));
    }

    @Test
    void enviarNotificacionDelegaEnElGestorDeEnvio() {
        Notificacion notificacion = notificacion();

        service.enviarNotificacion(notificacion, "email", "ana@test.com");

        verify(gestorEnvio).enviarNotificacion("email", "ana@test.com", notificacion);
    }

    @Test
    void lasTransaccionesEstanEnElService() throws Exception {
        assertNotNull(NotificadorService.class.getMethod("procesarSolicitudDeNotificacion",
                SolicitudNotificacionDTO.class).getAnnotation(Transactional.class));
        assertNotNull(NotificadorService.class.getMethod("reclamarParaEnvio",
                UUID.class, String.class, String.class).getAnnotation(Transactional.class));
        assertNotNull(NotificadorService.class.getMethod("marcarEnviada", UUID.class, UUID.class)
                .getAnnotation(Transactional.class));
        assertNotNull(NotificadorService.class.getMethod("marcarFallida", UUID.class, UUID.class)
                .getAnnotation(Transactional.class));
    }
}
