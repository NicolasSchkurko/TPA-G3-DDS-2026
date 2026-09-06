package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioDeResultadosMatchmaking;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GestorMatchmaking {
    private RepositorioDeResultadosMatchmaking repositorioDeResultadosMatchmaking;

    public GestorMatchmaking(RepositorioDeResultadosMatchmaking repositorioDeResultadosMatchmaking){
        this.repositorioDeResultadosMatchmaking=repositorioDeResultadosMatchmaking;
    }

    public PropuestaAsignacion obtenerPropuestaSeleccionadaParaDonacion(UUID donacionId, Integer posicion){
        ResultadoMatchmaking resultado = repositorioDeResultadosMatchmaking.findByDonacionId(donacionId).orElseThrow(() -> new IllegalArgumentException(
                        "No hay resultado de matchmaking para la donación " + donacionId
                )
        );

        if (posicion == null || posicion < 0 || posicion >= resultado.getPropuestasOrdenadas().size()) {
            throw new IllegalArgumentException("Posición de propuesta inválida");
        }

        PropuestaAsignacion propuesta = resultado.getPropuestasOrdenadas().get(posicion);
        return  propuesta;
    }
}
