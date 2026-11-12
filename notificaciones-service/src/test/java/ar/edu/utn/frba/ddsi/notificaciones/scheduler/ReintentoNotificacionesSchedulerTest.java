package ar.edu.utn.frba.ddsi.notificaciones.scheduler;

import ar.edu.utn.frba.ddsi.notificaciones.messaging.ProductorNotificaciones;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories.NotificacionReintentable;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories.RepositorioNotificaciones;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReintentoNotificacionesSchedulerTest {

    private final RepositorioNotificaciones repositorio = mock(RepositorioNotificaciones.class);
    private final ProductorNotificaciones productor = mock(ProductorNotificaciones.class);
    private final ReintentoNotificacionesScheduler scheduler =
            new ReintentoNotificacionesScheduler(repositorio, productor, 10, 3, 2);

    @Test
    void republicaLasPendientesAntiguasQueReclama() {
        NotificacionReintentable primera = nuevaNotificacion();
        NotificacionReintentable segunda = nuevaNotificacion();
        when(repositorio.findParaReintentar(
                eq(EstadoNotificacion.PENDIENTE), any(LocalDateTime.class), eq(3), any(Pageable.class)))
                .thenReturn(List.of(primera, segunda));
        when(repositorio.reclamarParaReintento(
                eq(primera.id()), eq(EstadoNotificacion.PENDIENTE), eq(0), eq(3)))
                .thenReturn(1);
        when(repositorio.reclamarParaReintento(
                eq(segunda.id()), eq(EstadoNotificacion.PENDIENTE), eq(0), eq(3)))
                .thenReturn(1);

        scheduler.reintentarPendientesAntiguas();

        verify(productor).enviar(primera.id(), primera.tipoMedioDeContacto(), primera.direccionDeContacto());
        verify(productor).enviar(segunda.id(), segunda.tipoMedioDeContacto(), segunda.direccionDeContacto());
    }

    @Test
    void siOtraInstanciaReclamaUnaNoLaPublica() {
        NotificacionReintentable ganada = nuevaNotificacion();
        NotificacionReintentable perdida = nuevaNotificacion();
        when(repositorio.findParaReintentar(
                eq(EstadoNotificacion.PENDIENTE), any(LocalDateTime.class), eq(3), any(Pageable.class)))
                .thenReturn(List.of(ganada, perdida));
        when(repositorio.reclamarParaReintento(
                eq(ganada.id()), eq(EstadoNotificacion.PENDIENTE), eq(0), eq(3)))
                .thenReturn(1);
        when(repositorio.reclamarParaReintento(
                eq(perdida.id()), eq(EstadoNotificacion.PENDIENTE), eq(0), eq(3)))
                .thenReturn(0);

        scheduler.reintentarPendientesAntiguas();

        verify(productor).enviar(ganada.id(), ganada.tipoMedioDeContacto(), ganada.direccionDeContacto());
        verify(productor, never()).enviar(perdida.id(), perdida.tipoMedioDeContacto(), perdida.direccionDeContacto());
    }

    @Test
    void usaElLimiteDeAntiguedadElTopeDeIntentosYElTamanoDelLote() {
        when(repositorio.findParaReintentar(
                eq(EstadoNotificacion.PENDIENTE), any(LocalDateTime.class), eq(3), any(Pageable.class)))
                .thenReturn(List.of());
        LocalDateTime piso = LocalDateTime.now().minusMinutes(10).minusSeconds(2);

        scheduler.reintentarPendientesAntiguas();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorio).findParaReintentar(
                eq(EstadoNotificacion.PENDIENTE), captor.capture(), eq(3), pagina.capture());
        assertTrue(captor.getValue().isAfter(piso));
        assertTrue(captor.getValue().isBefore(LocalDateTime.now()));
        assertEquals(100, pagina.getValue().getPageSize(), "el scheduler tiene que pedir un lote acotado");
    }

    @Test
    void siLaPublicacionFallaDevuelveElIntento() {
        NotificacionReintentable falla = nuevaNotificacion();
        when(repositorio.findParaReintentar(
                eq(EstadoNotificacion.PENDIENTE), any(LocalDateTime.class), eq(3), any(Pageable.class)))
                .thenReturn(List.of(falla));
        when(repositorio.reclamarParaReintento(
                eq(falla.id()), eq(EstadoNotificacion.PENDIENTE), eq(0), eq(3)))
                .thenReturn(1);
        doThrow(new RuntimeException("broker caído")).when(productor)
                .enviar(falla.id(), falla.tipoMedioDeContacto(), falla.direccionDeContacto());

        scheduler.reintentarPendientesAntiguas();

        verify(repositorio).compensarReintentoFallido(falla.id(), EstadoNotificacion.PENDIENTE);
    }

    @Test
    void liberaEnviosColgadosAntesDeConsultar() {
        when(repositorio.findParaReintentar(
                eq(EstadoNotificacion.PENDIENTE), any(LocalDateTime.class), eq(3), any(Pageable.class)))
                .thenReturn(List.of());
        LocalDateTime esperado = LocalDateTime.now().minusMinutes(2);

        scheduler.reintentarPendientesAntiguas();

        ArgumentCaptor<LocalDateTime> corte = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repositorio).liberarEnviosColgados(
                eq(EstadoNotificacion.PENDIENTE), eq(EstadoNotificacion.ENVIANDO), corte.capture());
        assertTrue(corte.getValue().isAfter(esperado.minusSeconds(2)));
        assertTrue(corte.getValue().isBefore(esperado.plusSeconds(2)));
    }

    @Test
    void siLaColaEstaVaciaNoRepublicaNada() {
        when(repositorio.findParaReintentar(
                eq(EstadoNotificacion.PENDIENTE), any(LocalDateTime.class), eq(3), any(Pageable.class)))
                .thenReturn(List.of());

        scheduler.reintentarPendientesAntiguas();

        verifyNoInteractions(productor);
    }

    private NotificacionReintentable nuevaNotificacion() {
        return new NotificacionReintentable(UUID.randomUUID(), 0, "email", "ana@test.com");
    }
}
