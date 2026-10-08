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

        // posicion es 1-based: así la setean AlgoritmoAsignacion.extraerRanking y
        // AsignadorDonaciones.obtenerInterseccion, y así la expone PropuestaAsignacionDTO al
        // front. Indexar con get(posicion) desfasaba en uno: "la propuesta número 1" de la
        // pantalla terminaba asignando la segunda de la lista.
        int size = resultado.getPropuestasOrdenadas().size();
        if (posicion == null || posicion < 1 || posicion > size) {
            throw new IllegalArgumentException("Posición de propuesta inválida");
        }

        PropuestaAsignacion propuesta = resultado.getPropuestasOrdenadas().get(posicion - 1);
        return  propuesta;
    }
}
