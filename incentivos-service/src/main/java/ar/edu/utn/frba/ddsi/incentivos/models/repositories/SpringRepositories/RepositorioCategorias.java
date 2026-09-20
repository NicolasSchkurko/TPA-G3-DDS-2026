package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
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

    Optional<Categoria> findByIdCategoria(UUID idCategoria);

    List<Categoria> findByPosicionSecuenciaGreaterThanEqual(Integer nivel);

    List<Categoria> findByPosicionSecuenciaBetween(Integer start, Integer end);

    List<Categoria> findAllByOrderByPosicionSecuenciaAsc();


    Optional<Categoria> findFirstByPosicionSecuenciaGreaterThanOrderByPosicionSecuenciaAsc(Integer posicionActual);

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

    // Metodos que enmascaran otros metodos solo para mejor legibilidad.
    default List<Categoria> obtenerTodas() {
        return this.findAllByOrderByPosicionSecuenciaAsc();
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