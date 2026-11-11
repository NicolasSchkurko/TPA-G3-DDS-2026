package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repositorio Spring Data JPA para ResultadoMatchmaking.
 * No se usa directamente desde los Gestores/Services: RepositorioDeResultadosMatchmaking actúa
 * como fachada, manteniendo los métodos públicos que ya existían cuando era un repositorio en memoria.
 */
public interface RepositorioDeResultadosMatchmaking extends JpaRepository<ResultadoMatchmaking, UUID> {
    Optional<ResultadoMatchmaking> findByDonacion_Id(UUID donacionId);

    default void guardar(ResultadoMatchmaking resultado) {
        if (resultado == null || resultado.getDonacion() == null || resultado.getDonacion().getId() == null) {
            throw new IllegalArgumentException("El resultado de matchmaking debe tener una donación con ID válido.");
        }
        save(resultado);
    }

    default void guardarResultados(List<ResultadoMatchmaking> resultados) { saveAll(resultados); }

    default void eliminarResultado(ResultadoMatchmaking resultadoAEliminar) {
        delete(resultadoAEliminar);
        flush();
    }

    default Optional<ResultadoMatchmaking> findByDonacionId(UUID donacionId) {
        return findByDonacion_Id(donacionId);
    }

}
