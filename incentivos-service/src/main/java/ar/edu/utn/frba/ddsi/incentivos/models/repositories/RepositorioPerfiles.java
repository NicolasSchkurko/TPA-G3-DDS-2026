package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import java.util.List;
import org.springdoc.core.converters.models.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioPerfiles extends JpaRepository<Perfil, UUID> {

    Optional<Perfil> findByIdUsuario(UUID idUsuario);

    @Query("SELECT io.insignia FROM Perfil p JOIN p.insigniasObtenidas io WHERE p.idUsuario = :idUsuario")
    List<Insignia> obtenerInsigniasPorIdUsuario(@Param("idUsuario") UUID idUsuario);

    @Query("SELECT pm.mision FROM Perfil p JOIN p.progresoMisionActual pm WHERE p.idUsuario = :idUsuario")
    Optional<Mision> obtenerMisionPorIdUsuario(@Param("idUsuario") UUID idUsuario);

    boolean existsByIdUsuario(UUID idUsuario);

    Optional<Perfil> findByNombreUsuario(String nombreUsuario);

    List<Perfil> findAllByCategoriaActual(Categoria categoria);

    @Query("SELECT p, COUNT(io) as total " +
        "FROM Perfil p JOIN p.insigniasObtenidas io " +
        "WHERE MONTH(io.fechaObtencion) = :mes AND YEAR(io.fechaObtencion) = :anio " +
        "GROUP BY p.idPerfil " +
        "ORDER BY total DESC " +
        "FETCH FIRST 10 ROWS ONLY")
    List<Object[]> calcularRankingMensual(@Param("mes") int mes, @Param("anio") int anio);

    Page<Perfil> findAll(Pageable pageable); // paginación
}