package ar.edu.utn.frba.ddsi.donaciones.donacion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.donaciones.messaging.ProductorLogistica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorAsignaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorFormulario;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeBienes;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeDonantes;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeFormularios;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeResultadosMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorCatalogo;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;

@DisplayName("DonacionService")
class DonacionServiceTest {

  @Nested
  @DisplayName("Barrido de matchmaking")
  class BarridoMatchmaking {

    @Test
    @DisplayName("usa ids y un único conteo agregado por corrida, no findAll")
    void barridoNoCargaTablasEnteras() {
      RepositorioDeDonaciones repoDonaciones = mock(RepositorioDeDonaciones.class);
      RepositorioDeEntidadesBeneficiarias repoEntidades = mock(RepositorioDeEntidadesBeneficiarias.class);

      UUID primera = UUID.randomUUID();
      UUID segunda = UUID.randomUUID();
      Donacion donacion1 = new Donacion();
      donacion1.setId(primera);
      Donacion donacion2 = new Donacion();
      donacion2.setId(segunda);
      when(repoDonaciones.buscarIdsSinAsignar()).thenReturn(List.of(primera, segunda));
      when(repoDonaciones.obtenerPorId(primera)).thenReturn(Optional.of(donacion1));
      when(repoDonaciones.obtenerPorId(segunda)).thenReturn(Optional.of(donacion2));
      when(repoDonaciones.contarEntregadasUltimoTrimestrePorEntidad(any())).thenReturn(Map.of());
      when(repoEntidades.obtenerTodasActivas()).thenReturn(List.of());

      DonacionService service = service(
          repoDonaciones, repoEntidades, mock(RepositorioDeResultadosMatchmaking.class));

      service.ejecutarMatchmakingADemanda();

      verify(repoDonaciones, times(1)).buscarIdsSinAsignar();
      verify(repoDonaciones, times(1)).contarEntregadasUltimoTrimestrePorEntidad(any());
      verify(repoDonaciones, never()).buscarDonacionesSinAsignar();
      verify(repoEntidades, times(2)).obtenerTodasActivas();
      verify(repoEntidades, never()).obtenerTodas();
    }
  }

  @Nested
  @DisplayName("Eliminar donación")
  class EliminarDonacion {

    @Test
    @DisplayName("borra primero el resultado de matchmaking y después la donación")
    void eliminarDonacionBorraElResultadoPrimero() {
      RepositorioDeDonaciones repoDonaciones = mock(RepositorioDeDonaciones.class);
      RepositorioDeResultadosMatchmaking repoResultados =
          mock(RepositorioDeResultadosMatchmaking.class);
      UUID id = UUID.randomUUID();
      ResultadoMatchmaking resultado = mock(ResultadoMatchmaking.class);
      when(repoResultados.findByDonacionId(id)).thenReturn(Optional.of(resultado));

      DonacionService service = service(
          repoDonaciones, mock(RepositorioDeEntidadesBeneficiarias.class), repoResultados);

      service.eliminarDonacion(id);

      InOrder orden = inOrder(repoResultados, repoDonaciones);
      orden.verify(repoResultados).eliminarResultado(resultado);
      orden.verify(repoDonaciones).eliminarPorId(id);
    }
  }

  private static DonacionService service(RepositorioDeDonaciones repoDonaciones,
                                         RepositorioDeEntidadesBeneficiarias repoEntidades,
                                         RepositorioDeResultadosMatchmaking repoResultados) {
    return new DonacionService(
        mock(GestorAsignaciones.class),
        mock(GestorFormulario.class),
        mock(GestorMatchmaking.class),
        mock(GestorCatalogo.class),
        repoDonaciones,
        mock(RepositorioDeDonantes.class),
        mock(RepositorioDeFormularios.class),
        repoEntidades,
        repoResultados,
        mock(RepositorioDeBienes.class),
        mock(ProductorLogistica.class),
        mock(PlatformTransactionManager.class));
  }
}
