package ar.edu.utn.frba.ddsi.donaciones.logistica;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Persona;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorEventosLogistica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioDonaciones;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

  @AfterEach
  void tearDown() {
    TransactionSynchronizationManager.clear();
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

  @Test
  @DisplayName("Punto 61: Si incentivos-service falla en ENTREGA_CONFIRMADA, la donación debe guardarse como ENTREGADO sin fallar procesarEvento")
  void manejarEntregaConfirmada_siIncentivosFalla_persisteDonacionEntregadaYNoPropagaError() {
    UUID idDonacion = UUID.randomUUID();
    Donacion donacion = new Donacion();
    donacion.setId(idDonacion);
    donacion.actualizarEstado(Estado.EN_TRASLADO, "En viaje");

    SubcategoriaBien subcategoria = mock(SubcategoriaBien.class);
    CategoriaBien categoria = mock(CategoriaBien.class);
    when(subcategoria.getNombre()).thenReturn("Almacenes");
    when(subcategoria.getCategoria()).thenReturn(categoria);
    when(categoria.getNombre()).thenReturn("Alimentos");
    donacion.setSubcategoria(subcategoria);

    EntidadBeneficiaria entidad = mock(EntidadBeneficiaria.class);
    Juridica juridica = mock(Juridica.class);
    when(entidad.getPersonaJuridica()).thenReturn(juridica);
    when(juridica.getRazonSocial()).thenReturn("Fundación Test");
    donacion.setEntidad(entidad);

    Donante donante = mock(Donante.class);
    when(donante.getId()).thenReturn(UUID.randomUUID());
    when(donante.getPersona()).thenReturn(mock(Persona.class));
    donacion.setDonante(donante);

    when(repositorioDonaciones.obtenerPorId(idDonacion)).thenReturn(Optional.of(donacion));

    doThrow(new RuntimeException("Incentivos timeout / 500"))
        .when(incentivosClient).notificarImpactoDonacion(any(), any());

    EventoLogisticaDTO evento = new EventoLogisticaDTO();
    evento.setId(201L);
    evento.setTipoEvento("ENTREGA_CONFIRMADA");
    evento.setReferenciaId(idDonacion.toString());
    evento.setPayloadJson("{\"fechaEntrega\":\"2026-10-07T14:30:00\"}");

    assertDoesNotThrow(() -> gestor.procesarEvento(evento, Collections.emptyList()));

    ArgumentCaptor<Donacion> captor = ArgumentCaptor.forClass(Donacion.class);
    verify(repositorioDonaciones, times(1)).guardar(captor.capture());
    assertEquals(Estado.ENTREGADO, captor.getValue().getEstado());
  }

  @Test
  @DisplayName("Punto 61: En contexto transaccional, la notificación a incentivos se registra en afterCommit")
  void manejarEntregaConfirmada_enTransaccion_registraAfterCommit() {
    TransactionSynchronizationManager.initSynchronization();
    try {
      TransactionSynchronizationManager.setActualTransactionActive(true);

      UUID idDonacion = UUID.randomUUID();
      Donacion donacion = new Donacion();
      donacion.setId(idDonacion);
      donacion.actualizarEstado(Estado.EN_TRASLADO, "En viaje");

      SubcategoriaBien subcategoria = mock(SubcategoriaBien.class);
      CategoriaBien categoria = mock(CategoriaBien.class);
      when(subcategoria.getNombre()).thenReturn("Almacenes");
      when(subcategoria.getCategoria()).thenReturn(categoria);
      when(categoria.getNombre()).thenReturn("Alimentos");
      donacion.setSubcategoria(subcategoria);

      EntidadBeneficiaria entidad = mock(EntidadBeneficiaria.class);
      Juridica juridica = mock(Juridica.class);
      when(entidad.getPersonaJuridica()).thenReturn(juridica);
      when(juridica.getRazonSocial()).thenReturn("Fundación Test");
      donacion.setEntidad(entidad);

      Donante donante = mock(Donante.class);
      when(donante.getId()).thenReturn(UUID.randomUUID());
      when(donante.getPersona()).thenReturn(mock(Persona.class));
      donacion.setDonante(donante);

      when(repositorioDonaciones.obtenerPorId(idDonacion)).thenReturn(Optional.of(donacion));

      EventoLogisticaDTO evento = new EventoLogisticaDTO();
      evento.setId(202L);
      evento.setTipoEvento("ENTREGA_CONFIRMADA");
      evento.setReferenciaId(idDonacion.toString());
      evento.setPayloadJson("{\"fechaEntrega\":\"2026-10-07T14:30:00\"}");

      gestor.procesarEvento(evento, Collections.emptyList());

      // Antes del commit, incentivos no debe haber sido invocado
      verify(incentivosClient, never()).notificarImpactoDonacion(any(), any());

      // Simulamos afterCommit (e incluso con fallo en incentivos)
      doThrow(new RuntimeException("Incentivos timeout en afterCommit"))
          .when(incentivosClient).notificarImpactoDonacion(any(), any());

      for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
        sync.afterCommit();
      }

      verify(incentivosClient, times(1)).notificarImpactoDonacion(eq(donante.getId()), any());
    } finally {
      TransactionSynchronizationManager.clear();
    }
  }
}
