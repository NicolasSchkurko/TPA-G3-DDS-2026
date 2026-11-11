package ar.edu.utn.frba.ddsi.donaciones.messaging;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.donaciones.exceptions.EventoYaProcesadoException;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorEventosLogistica;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeAdministradores;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeEventosProcesados;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class EventosListenerTest {

  private RepositorioDeAdministradores repoAdministradores;
  private RepositorioDeEventosProcesados repoEventosProcesados;
  private GestorEventosLogistica gestorLogistica;
  private EventosListener listener;

  @BeforeEach
  void setUp() {
    repoAdministradores = mock(RepositorioDeAdministradores.class);
    repoEventosProcesados = mock(RepositorioDeEventosProcesados.class);
    gestorLogistica = mock(GestorEventosLogistica.class);
    listener = new EventosListener(repoAdministradores, repoEventosProcesados, gestorLogistica);
  }

  private static EventoLogisticaDTO evento(long id) {
    EventoLogisticaDTO evento = new EventoLogisticaDTO();
    evento.setId(id);
    evento.setTipoEvento("ENTREGA_CONFIRMADA");
    return evento;
  }

  @Test
  @DisplayName("Punto 45: EventosListener no debe ser @Transactional en recibirEvento")
  void verificarTransaccionalidadAislada() throws NoSuchMethodException {
    Method recibirEventoMethod = EventosListener.class.getMethod("recibirEvento", EventoLogisticaDTO.class);
    assertFalse(recibirEventoMethod.isAnnotationPresent(Transactional.class),
        "recibirEvento NO debe ser @Transactional para evitar UnexpectedRollbackException fuera del try/catch");

  }

  @Test
  @DisplayName("Punto 79: solo carga administradores para ENTREGA_FALLIDA")
  void soloCargaAdminsParaEntregaFallida() {
    long idConfirmada = 1L;
    when(repoEventosProcesados.existe(idConfirmada)).thenReturn(false);
    listener.recibirEvento(evento(idConfirmada));
    verify(repoAdministradores, never()).obtenerTodos();

    long idFallida = 2L;
    when(repoEventosProcesados.existe(idFallida)).thenReturn(false);
    EventoLogisticaDTO fallida = evento(idFallida);
    fallida.setTipoEvento("ENTREGA_FALLIDA");
    listener.recibirEvento(fallida);
    verify(repoAdministradores, times(1)).obtenerTodos();
  }

  @Test
  @DisplayName("Punto 75: Evento sin id se descarta sin tocar repositorios ni gestor")
  void recibirEvento_sinId_noTocaRepositorios() {
    EventoLogisticaDTO evento = new EventoLogisticaDTO(); // id null

    listener.recibirEvento(evento);

    verify(repoEventosProcesados, never()).existe(anyLong());
    verify(repoAdministradores, never()).obtenerTodos();
    verify(gestorLogistica, never()).procesarEvento(any(), any());
  }

  @Test
  @DisplayName("Punto 75: Evento ya procesado (fila en la tabla de deduplicación) se confirma sin reprocesar")
  void recibirEvento_duplicado_noProcesaNiRegistraOtraVez() {
    long id = 42L;
    when(repoEventosProcesados.existe(id)).thenReturn(true);

    listener.recibirEvento(evento(id));

    verify(repoAdministradores, never()).obtenerTodos();
    verify(gestorLogistica, never()).procesarEvento(any(), any());
  }

  @Test
  @DisplayName("Punto 75: Un fallo del procesamiento (aunque sea de persistencia) se propaga para retry: el rollback deshizo el reclamo")
  void recibirEvento_siFalla_propagaLaExcepcionParaRetry() {
    long id = 42L;
    when(repoEventosProcesados.existe(id)).thenReturn(false);
    when(repoAdministradores.obtenerTodos()).thenReturn(List.of());

    // Ojo: no toda DataIntegrityViolationException es una carrera de deduplicación. Solo
    // EventoYaProcesadoException se confirma; el resto tiene que reintentarse.
    DataIntegrityViolationException falloDePersistencia =
        new DataIntegrityViolationException("Constraint violation al guardar la donación");
    doThrow(falloDePersistencia)
        .when(gestorLogistica).procesarEvento(any(EventoLogisticaDTO.class), any());

    DataIntegrityViolationException propagada = assertThrows(
        DataIntegrityViolationException.class, () -> listener.recibirEvento(evento(id)));

    assertSame(falloDePersistencia, propagada);
    verify(gestorLogistica, times(1)).procesarEvento(
        argThat(e -> e != null && e.getId() == id), any());
  }

  @Test
  @DisplayName("Punto 75: Si el procesamiento es exitoso, el gestor reclama y procesa el evento")
  void recibirEvento_siExitoso_procesaUnaSolaVez() {
    long id = 42L;
    when(repoEventosProcesados.existe(id)).thenReturn(false);
    when(repoAdministradores.obtenerTodos()).thenReturn(List.of());

    listener.recibirEvento(evento(id));

    verify(gestorLogistica, times(1)).procesarEvento(
        argThat(e -> e != null && e.getId() == id), any());
  }

  @Test
  @DisplayName("Punto 75 (clúster): Carrera de réplicas — si otra instancia ya reclamó el evento, "
      + "se confirma el mensaje sin propagar la excepción (no se reintenta)")
  void recibirEvento_carreraReplicas_siInsertaOtraInstancia_noSeReintenta() {
    long id = 42L;
    when(repoEventosProcesados.existe(id)).thenReturn(false);
    when(repoAdministradores.obtenerTodos()).thenReturn(List.of());

    // El gestor reclama la fila al inicio de su transacción; si otra réplica ganó la
    // carrera, la PK duplicada se traduce a EventoYaProcesadoException.
    doThrow(new EventoYaProcesadoException(id, new DataIntegrityViolationException("Duplicate entry")))
        .when(gestorLogistica).procesarEvento(any(EventoLogisticaDTO.class), any());

    // No se debe propagar ninguna excepción: la carrera se resuelve confirmando el mensaje.
    assertDoesNotThrow(() -> listener.recibirEvento(evento(id)));

    verify(gestorLogistica, times(1)).procesarEvento(
        argThat(e -> e != null && e.getId() == id), any());
    verify(repoEventosProcesados, times(1)).existe(id);
  }
}
