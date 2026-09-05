package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioRankings extends JpaRepository<RankingMensual, UUID> {

    // Spring Data crea automáticamente esta consulta por nosotros
    Optional<RankingMensual> findByPeriodo(YearMonth periodo);

    // Todos los demás métodos (save, findById, findAll) ya vienen heredados de JpaRepository
}