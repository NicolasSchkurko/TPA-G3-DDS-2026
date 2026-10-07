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

    /**
     * Devuelve la propuesta que el operador eligio por su posicion, tal como la devolvio
     * {@code GET /donaciones/pendientes}.
     */
    public PropuestaAsignacion obtenerPropuestaSeleccionadaParaDonacion(UUID donacionId, Integer posicion){
        ResultadoMatchmaking resultado = repositorioDeResultadosMatchmaking
                .findByDonacionId(donacionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay resultado de matchmaking para la donacion " + donacionId
                ));

        // 1-based: la primera propuesta es la posicion 1, tal como la numeran los algoritmos.
        if (posicion == null || posicion < 1 || posicion > resultado.getPropuestasOrdenadas().size()) {
            throw new IllegalArgumentException("Posicion de propuesta invalida: " + posicion
                    + " (las propuestas van de 1 a " + resultado.getPropuestasOrdenadas().size() + ")");
        }

        PropuestaAsignacion propuesta = resultado.getPropuestasOrdenadas().get(posicion - 1);
        return propuesta;
    }
}
