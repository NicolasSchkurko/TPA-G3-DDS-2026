package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AsignadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDeResultadosMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDonaciones;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GestorMatchmaking {
    private RepositorioDeResultadosMatchmaking repositorioDeResultadosMatchmaking;

    public List<ResultadoMatchmaking> obtenerTodosLosResultadosMatchmaking() {
        return repositorioDeResultadosMatchmaking.findAll();
    }

    public Donacion asignarPropuesta(UUID donacionId, Integer posicion) {
        ResultadoMatchmaking resultado = repositorioDeResultadosMatchmaking.findByDonacionId(donacionId).orElseThrow(() -> new IllegalArgumentException(
                        "No hay resultado de matchmaking para la donación " + donacionId
                )
        );

        if (posicion == null || posicion < 0 || posicion >= resultado.getPropuestasOrdenadas().size()) {
            throw new IllegalArgumentException("Posición de propuesta inválida");
        }

        PropuestaAsignacion propuesta = resultado.getPropuestasOrdenadas().get(posicion);

        Donacion donacion = resultado.getDonacion();

        if (donacion.getEstado() != Estado.PENDIENTE_ASIGNACION) {
            throw new IllegalStateException("La donación ya está asignada");
        }

        AsignadorDonaciones.asignarDonacionAPropuesta(donacion, propuesta);

        repositorioDeResultadosMatchmaking.eliminarResultado(resultado);

        return donacion;
    }
    public void guardarResultados(List<ResultadoMatchmaking> resultados){
        repositorioDeResultadosMatchmaking.guardarResultados(resultados);
    }
}
