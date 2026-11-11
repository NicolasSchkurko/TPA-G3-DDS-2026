package ar.edu.utn.frba.ddsi.donaciones.repositories;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeResultadosMatchmaking;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Punto 64: re-matchmaking reemplaza el resultado previo en lugar de colisionar")
class RepositorioDeResultadosMatchmakingTest {

    @Test
    @DisplayName("guardar es un save puro: no borra ni verifica existencia (el reemplazo ocurre antes, en procesarMatchmaking)")
    void guardar_esUnSavePuro_noBorra() {
        RepositorioDeResultadosMatchmaking repo = mock(RepositorioDeResultadosMatchmaking.class, org.mockito.Mockito.CALLS_REAL_METHODS);

        UUID donacionId = UUID.randomUUID();
        Donacion donacion = new Donacion();
        donacion.setId(donacionId);

        ResultadoMatchmaking nuevo = new ResultadoMatchmaking(donacion, List.of(), true);

        repo.guardar(nuevo);

        verify(repo, never()).delete(org.mockito.ArgumentMatchers.any());
        verify(repo, times(1)).save(nuevo);
    }

    @Test
    @DisplayName("si no existe resultado previo, solo se guarda")
    void guardar_sinResultadoExistente_soloGuarda() {
        RepositorioDeResultadosMatchmaking repo = mock(RepositorioDeResultadosMatchmaking.class, org.mockito.Mockito.CALLS_REAL_METHODS);

        UUID donacionId = UUID.randomUUID();
        Donacion donacion = new Donacion();
        donacion.setId(donacionId);

        ResultadoMatchmaking nuevo = new ResultadoMatchmaking(donacion, List.of(), true);

        when(repo.findByDonacion_Id(donacionId)).thenReturn(Optional.empty());

        repo.guardar(nuevo);

        verify(repo, never()).delete(org.mockito.ArgumentMatchers.any());
        verify(repo, times(1)).save(nuevo);
    }
}
