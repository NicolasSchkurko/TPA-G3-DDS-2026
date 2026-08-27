package ar.edu.utn.frba.ddsi.donaciones.models.repositories;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RepositorioDeResultadosMatchmaking {
    private final List<ResultadoMatchmaking> resultadosMatchmakings = new ArrayList<>();

    public List<ResultadoMatchmaking> findAll() {
        return new ArrayList<>(resultadosMatchmakings);
    }
    public void guardarResultados(List<ResultadoMatchmaking> resultados){
        this.resultadosMatchmakings.addAll(resultados);
    }

    public void eliminarResultado(ResultadoMatchmaking resultadoAEliminar){
        this.resultadosMatchmakings.remove(resultadoAEliminar);
    }

    public Optional<ResultadoMatchmaking> findByDonacionId(UUID donacionId){
       return this.resultadosMatchmakings.stream()
                .filter(p -> p.getDonacion().getId().equals(donacionId))
                .findFirst();
    }

}
