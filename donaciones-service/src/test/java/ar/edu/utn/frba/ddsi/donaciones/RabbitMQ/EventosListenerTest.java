package ar.edu.utn.frba.ddsi.donaciones.RabbitMQ;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorEventosLogistica;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioAdministradores;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EventosListenerTest {

  private RepositorioAdministradores repoAdministradores;
  private GestorEventosLogistica gestorLogistica;
  private EventosListener listener;

  @BeforeEach
  void setUp() {
    repoAdministradores = mock(RepositorioAdministradores.class);
    gestorLogistica = mock(GestorEventosLogistica.class);
    listener = new EventosListener(repoAdministradores, gestorLogistica);
  }

  @Test
  @DisplayName("Punto 45: EventosListener no debe ser @Transactional en recibirEvento pero GestorEventosLogistica.procesarEvento sí debe serlo")
  void verificarTransaccionalidadAislada() throws NoSuchMethodException {
    Method recibirEventoMethod = EventosListener.class.getMethod("recibirEvento", EventoLogisticaDTO.class);
    assertFalse(recibirEventoMethod.isAnnotationPresent(Transactional.class),
        "recibirEvento NO debe ser @Transactional para evitar UnexpectedRollbackException fuera del try/catch");

    Method procesarEventoMethod = GestorEventosLogistica.class.getMethod("procesarEvento", EventoLogisticaDTO.class, List.class);
    assertTrue(procesarEventoMethod.isAnnotationPresent(Transactional.class),
        "GestorEventosLogistica.procesarEvento DEBE ser @Transactional para manejar transacciones atómicas por evento");
  }

  @Test
  @DisplayName("Punto 45: Si el procesamiento falla, el cursor ultimoIdProcesado NO se avanza y la excepción se propaga para retry")
  void recibirEvento_siFalla_noAvanzaCursorYLanzaExcepcion() {
    EventoLogisticaDTO evento = new EventoLogisticaDTO();
    evento.setId(42L);
    evento.setTipoEvento("ENTREGA_CONFIRMADA");

    doThrow(new RuntimeException("Simulated DB constraint failure"))
        .when(gestorLogistica).procesarEvento(eq(evento), any());

    assertThrows(RuntimeException.class, () -> listener.recibirEvento(evento));

    assertEquals(0L, listener.getUltimoIdProcesado(),
        "El cursor no debe avanzar si la transacción falló, permitiendo que el retry lo procese");
  }

  @Test
  @DisplayName("Punto 45: Si el procesamiento es exitoso, el cursor avanza correctamente")
  void recibirEvento_siExitoso_avanzaCursor() {
    EventoLogisticaDTO evento = new EventoLogisticaDTO();
    evento.setId(42L);
    evento.setTipoEvento("ENTREGA_CONFIRMADA");

    listener.recibirEvento(evento);

    assertEquals(42L, listener.getUltimoIdProcesado());
    verify(gestorLogistica, times(1)).procesarEvento(eq(evento), any());
  }
}
