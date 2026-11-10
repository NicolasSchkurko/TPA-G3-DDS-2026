package ar.edu.utn.frba.ddsi.donaciones.matchmaking;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AsignadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorAsignaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioDeResultadosMatchmaking;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Punto 64: re-matchmaking limpia el resultado previo antes de calcular")
class AsignadorDonacionesReMatchmakingTest {

    @Test
    @DisplayName("si ya existe un resultado para la donación, se borra antes de procesar (incluso sin propuestas)")
    void procesarMatchmaking_limpiaResultadoPrevio() {
        UUID donacionId = UUID.randomUUID();
        Donacion donacion = new Donacion();
        donacion.setId(donacionId);

        RepositorioDeResultadosMatchmaking repo = mock(RepositorioDeResultadosMatchmaking.class);
        ResultadoMatchmaking existente = mock(ResultadoMatchmaking.class);
        when(repo.findByDonacionId(donacionId)).thenReturn(Optional.of(existente));

        AsignadorDonaciones asignador = new AsignadorDonaciones(
                mock(GestorMatchmaking.class),
                mock(GestorAsignaciones.class),
                repo
        );

        // Sin entidades compatibles → sin propuestas → early return, pero el cleanup ya ocurrió en el bucle.
        asignador.ejecutarMatchmakingBatch(List.of(donacion), List.of());

        verify(repo, times(1)).eliminarResultado(existente);
        verify(repo, never()).guardar(org.mockito.ArgumentMatchers.any());
    }
}
