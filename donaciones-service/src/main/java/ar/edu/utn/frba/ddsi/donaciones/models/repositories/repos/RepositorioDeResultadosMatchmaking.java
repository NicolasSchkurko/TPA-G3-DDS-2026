package ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.ResultadoMatchmakingJpaRepository;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fachada sobre ResultadoMatchmakingJpaRepository (Spring Data JPA).
 * Mantiene la misma interfaz pública que tenía cuando era un repositorio en memoria.
 */
@Repository
public class RepositorioDeResultadosMatchmaking {

    private final ResultadoMatchmakingJpaRepository jpaRepository;

    public RepositorioDeResultadosMatchmaking(ResultadoMatchmakingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public List<ResultadoMatchmaking> findAll() {
        return jpaRepository.findAll();
    }

    public void guardar(ResultadoMatchmaking resultado) {
        if (resultado == null || resultado.getDonacion() == null || resultado.getDonacion().getId() == null) {
            throw new IllegalArgumentException("El resultado de matchmaking debe tener una donación con ID válido.");
        }
        if (findByDonacionId(resultado.getDonacion().getId()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un resultado de matchmaking para la donación: " + resultado.getDonacion().getId());
        }
        jpaRepository.save(resultado);
    }

    public void guardarResultados(List<ResultadoMatchmaking> resultados) {
        jpaRepository.saveAll(resultados);
    }

    public void eliminarResultado(ResultadoMatchmaking resultadoAEliminar) {
        jpaRepository.delete(resultadoAEliminar);
    }

    public Optional<ResultadoMatchmaking> findByDonacionId(UUID donacionId) {
        return jpaRepository.findByDonacion_Id(donacionId);
    }
}
