package ar.edu.utn.frba.ddsi.donaciones.logistica;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorEventosLogistica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioDonaciones;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class GestorEventosLogisticaTest {

  private RepositorioDonaciones repositorioDonaciones;
  private FabricaEstrategiasNotificacion fabricaEstrategias;
  private IncentivosClient incentivosClient;
  private ObjectMapper objectMapper;
  private GestorEventosLogistica gestor;

  @BeforeEach
  void setUp() {
    repositorioDonaciones = mock(RepositorioDonaciones.class);
    fabricaEstrategias = mock(FabricaEstrategiasNotificacion.class);
    incentivosClient = mock(IncentivosClient.class);
    objectMapper = new ObjectMapper();

    gestor = new GestorEventosLogistica(
        repositorioDonaciones,
        fabricaEstrategias,
        incentivosClient,
        objectMapper
    );
  }

  @Test
  @DisplayName("Punto 47: REINGRESO_DEPOSITO debe dejar la donación en EN_DEPOSITO para que el scheduler la recoja en matchmaking")
  void manejarReingresoDeposito_actualizaEstadoAEnDeposito() {
    UUID idDonacion = UUID.randomUUID();
    Donacion donacion = new Donacion();
    donacion.setId(idDonacion);
    donacion.actualizarEstado(Estado.ASIGNADO, "Asignado previamente");

    when(repositorioDonaciones.obtenerPorId(idDonacion)).thenReturn(Optional.of(donacion));

    EventoLogisticaDTO evento = new EventoLogisticaDTO();
    evento.setId(101L);
    evento.setTipoEvento("REINGRESO_DEPOSITO");
    evento.setReferenciaId(idDonacion.toString());

    gestor.procesarEvento(evento, Collections.emptyList());

    ArgumentCaptor<Donacion> captor = ArgumentCaptor.forClass(Donacion.class);
    verify(repositorioDonaciones, times(1)).guardar(captor.capture());

    Donacion guardada = captor.getValue();
    assertEquals(Estado.EN_DEPOSITO, guardada.getEstado(),
        "La donación que reingresa a depósito debe quedar en EN_DEPOSITO, no en PENDIENTE_ASIGNACION, para poder ser re-matcheada");
  }
}
