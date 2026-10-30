package ar.edu.utn.frba.ddsi.donaciones.personas;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.*;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

public class DonanteServiceTest {

  private RepositorioPersonas repositorioPersonas;
  private RepositorioCiudades repositorioCiudades;
  private FabricaEstrategiasNotificacion fabricaEstrategias;
  private IncentivosClient incentivosClient;
  private RepositorioDonantes repositorioDonantes;
  private RepositorioDonaciones repositorioDonaciones;
  private RepositorioFormularios repositorioFormularios;
  private ExecutorService executorImportacion;
  private DonanteService donanteService;

  @BeforeEach
  void setUp() {
    repositorioPersonas = mock(RepositorioPersonas.class);
    repositorioCiudades = mock(RepositorioCiudades.class);
    fabricaEstrategias = mock(FabricaEstrategiasNotificacion.class);
    incentivosClient = mock(IncentivosClient.class);
    repositorioDonantes = mock(RepositorioDonantes.class);
    repositorioDonaciones = mock(RepositorioDonaciones.class);
    repositorioFormularios = mock(RepositorioFormularios.class);
    executorImportacion = mock(ExecutorService.class);

    donanteService = new DonanteService(
        repositorioPersonas,
        repositorioCiudades,
        fabricaEstrategias,
        incentivosClient,
        repositorioDonantes,
        repositorioDonaciones,
        repositorioFormularios,
        executorImportacion
    );
  }

  @Test
  @DisplayName("Punto 34 y 35: Dar de baja un donante desvincula donaciones y formularios ANTES de eliminar y luego notifica incentivos")
  void eliminarPersona_desvinculaRelacionesAntesDeBorradoLocal_yLuegoNotificaIncentivos() {
    UUID donanteId = UUID.randomUUID();
    Donante donanteMock = mock(Donante.class);
    when(repositorioDonantes.buscarPorId(donanteId)).thenReturn(Optional.of(donanteMock));

    donanteService.eliminarPersona(donanteId);

    // Verificar orden estricto de ejecución
    InOrder inOrder = inOrder(repositorioDonaciones, repositorioFormularios, repositorioDonantes, incentivosClient);
    inOrder.verify(repositorioDonaciones).desvincularDonante(donanteId);
    inOrder.verify(repositorioFormularios).desvincularDonante(donanteId);
    inOrder.verify(repositorioDonantes).eliminarPorId(donanteId);
    inOrder.verify(repositorioDonantes).flush();
    inOrder.verify(incentivosClient).eliminarPerfil(donanteId);
  }

  @Test
  @DisplayName("Punto 35: Si incentivos-service falla o está caído, la baja local no se aborta ni se interrumpe")
  void eliminarPersona_siIncentivosFalla_noAbortaBajaLocal() {
    UUID donanteId = UUID.randomUUID();
    Donante donanteMock = mock(Donante.class);
    when(repositorioDonantes.buscarPorId(donanteId)).thenReturn(Optional.of(donanteMock));

    doThrow(new RestClientException("503 Service Unavailable"))
        .when(incentivosClient).eliminarPerfil(donanteId);

    assertDoesNotThrow(() -> donanteService.eliminarPersona(donanteId));

    verify(repositorioDonaciones).desvincularDonante(donanteId);
    verify(repositorioFormularios).desvincularDonante(donanteId);
    verify(repositorioDonantes).eliminarPorId(donanteId);
    verify(repositorioDonantes).flush();
  }

  @Test
  @DisplayName("Punto 35: Si el donante no existe, no intenta desvincular ni llama a incentivos")
  void eliminarPersona_siDonanteNoExiste_noLlamaAIncentivosNiBorra() {
    UUID donanteId = UUID.randomUUID();
    when(repositorioDonantes.buscarPorId(donanteId)).thenReturn(Optional.empty());

    donanteService.eliminarPersona(donanteId);

    verify(repositorioDonaciones, never()).desvincularDonante(any());
    verify(repositorioFormularios, never()).desvincularDonante(any());
    verify(repositorioDonantes, never()).eliminarPorId(any());
    verify(incentivosClient, never()).eliminarPerfil(any());
  }
}
