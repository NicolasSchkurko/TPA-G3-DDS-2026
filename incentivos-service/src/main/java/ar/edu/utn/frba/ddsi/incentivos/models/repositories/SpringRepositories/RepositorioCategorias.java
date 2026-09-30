package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioCategorias extends JpaRepository<Categoria, UUID> {

    List<Categoria> findAllByOrderByPosicionSecuenciaAsc();

    Optional<Categoria> findFirstByPosicionSecuenciaGreaterThanOrderByPosicionSecuenciaAsc(Integer posicionActual);

    @Query("""
    SELECT DISTINCT c FROM Categoria c
    LEFT JOIN c.categoriaMisiones cm
    WHERE (:nombre IS NULL OR LOWER(c.nombre) LIKE :nombre)
      AND (:posicionSecuencia IS NULL OR c.posicionSecuencia = :posicionSecuencia)
      AND (:misionId IS NULL OR cm.mision.idMision = :misionId)
""")
    Page<Categoria> findAllByFiltros(
        @Param("nombre") String nombre,
        @Param("posicionSecuencia") Integer posicionSecuencia,
        @Param("misionId") UUID misionId,
        Pageable pageable
    );

    @Modifying
    @Query("UPDATE Categoria c SET c.posicionSecuencia = c.posicionSecuencia + 1 WHERE c.posicionSecuencia >= :inicio AND c.posicionSecuencia <= :fin")
    void desplazarHaciaAbajo(@Param("inicio") Integer inicio, @Param("fin") Integer fin);

    @Modifying
    @Query("UPDATE Categoria c SET c.posicionSecuencia = c.posicionSecuencia - 1 WHERE c.posicionSecuencia >= :inicio AND c.posicionSecuencia <= :fin")
    void desplazarHaciaArriba(@Param("inicio") Integer inicio, @Param("fin") Integer fin);

    @Modifying
    @Query("UPDATE Categoria c SET c.posicionSecuencia = c.posicionSecuencia + 1 WHERE c.posicionSecuencia >= :inicio")
    void desplazarHaciaAbajoDesde(@Param("inicio") Integer inicio);

    @Modifying
    @Query("UPDATE Categoria c SET c.posicionSecuencia = c.posicionSecuencia - 1 WHERE c.posicionSecuencia >= :inicio")
    void desplazarHaciaArribaDesde(@Param("inicio") Integer inicio);


    default Page<Categoria> obtenerTodas(String nombre, Integer posicionSecuencia, UUID misionId, Pageable pageable) {
        String patronNombre = (nombre != null && !nombre.isBlank())
                              ? "%" + nombre.trim().toLowerCase() + "%"
                              : null;

        return this.findAllByFiltros(patronNombre, posicionSecuencia, misionId, pageable);
    }

    default Categoria obtenerPorId(UUID id) {
        return this.findById(id).orElse(null);
    }

    default Categoria obtenerCategoriaSiguiente(Categoria categoriaActual) {
        if (categoriaActual == null || categoriaActual.getPosicionSecuencia() == null) {
            return null;
        }

        return findFirstByPosicionSecuenciaGreaterThanOrderByPosicionSecuenciaAsc(categoriaActual.getPosicionSecuencia())
            .orElse(null);
    }

}