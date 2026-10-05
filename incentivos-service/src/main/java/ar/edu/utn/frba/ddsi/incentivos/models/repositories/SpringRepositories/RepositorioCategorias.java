package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Consultas sobre las categorías y su secuencia de posiciones.
 *
 * <p>Además de las consultas, tiene los {@code UPDATE} en bloque que desplazan las
 * posiciones cuando entra, sale o se mueve una categoría. Son consultas y no código de
 * aplicación porque hay que mover varias filas de a uno en una sola sentencia.
 */
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

    // ===== Secuencia de posiciones (punto 19) =====
    // Todos los @Modifying llevan flushAutomatically para que el UPDATE masivo parta del
    // estado real de la base y no pise inserts que todavía estan en el contexto de
    // persistencia sin flushear.
    // NO se pone clearAutomatically a proposito: en actualizarCategoria el contexto
    // contiene la Categoria que se esta editando, y limpiarla la dejaria detached en
    // mitad de la operacion; el setPosicionSecuencia y el copiar de esa misma entidad
    // vendrían despues sobre un objeto desligado, y el save final terminaria haciendo
    // merge de un CategoriaMision que ya no esta gestionado.

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Categoria c SET c.posicionSecuencia = c.posicionSecuencia + 1 WHERE c.posicionSecuencia >= :inicio AND c.posicionSecuencia <= :fin")
    void desplazarHaciaAbajo(@Param("inicio") Integer inicio, @Param("fin") Integer fin);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Categoria c SET c.posicionSecuencia = c.posicionSecuencia - 1 WHERE c.posicionSecuencia >= :inicio AND c.posicionSecuencia <= :fin")
    void desplazarHaciaArriba(@Param("inicio") Integer inicio, @Param("fin") Integer fin);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Categoria c SET c.posicionSecuencia = c.posicionSecuencia + 1 WHERE c.posicionSecuencia >= :inicio")
    void desplazarHaciaAbajoDesde(@Param("inicio") Integer inicio);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Categoria c SET c.posicionSecuencia = c.posicionSecuencia - 1 WHERE c.posicionSecuencia >= :inicio")
    void desplazarHaciaArribaDesde(@Param("inicio") Integer inicio);

    /**
     * Posiciones ocupadas, ordenadas. Se usa en vez de {@code count()} porque la secuencia
     * asume que las posiciones van de 1 a N, y eso solo es cierto si no hay huecos ni
     * duplicados: con un hueco, {@code count()} da un limite superior mayor que la
     * posicion realmente ocupada y el desplazamiento mueve categorias que no deberia.
     */
    @Query("SELECT c.posicionSecuencia FROM Categoria c WHERE c.posicionSecuencia IS NOT NULL ORDER BY c.posicionSecuencia ASC")
    List<Integer> listarPosiciones();

    /** Si la posición ya está tomada por otra categoría (punto 19). */
    boolean existsByPosicionSecuencia(Integer posicionSecuencia);

    /**
     * Categorías que tienen esta misión en su secuencia. Es la tercera referencia que
     * bloquea borrar una misión (punto 18): {@code CategoriaMision} es el lado propietario
     * de la relación y no tiene cascada, así que borrar una misión que pertenece a una
     * categoría revienta por FK si nadie la saca antes.
     */
    List<Categoria> findAllByCategoriaMisionesMision(Mision mision);

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

        return findFirstByPosicionSecuenciaGreaterThanOrderByPosicionSecuenciaAsc(
                categoriaActual.getPosicionSecuencia())
                .orElse(null);
    }

}
