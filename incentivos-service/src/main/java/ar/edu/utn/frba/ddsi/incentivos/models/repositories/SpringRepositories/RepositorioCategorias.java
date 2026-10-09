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
 * Consultas sobre las categorías y su secuencia de posiciones, incluidos los {@code UPDATE}
 * en bloque que desplazan posiciones en una sola sentencia.
 */
@Repository
public interface RepositorioCategorias extends JpaRepository<Categoria, UUID> {

    List<Categoria> findAllByOrderByPosicionSecuenciaAsc();

    /**
     * La categoría base (la primera) con su secuencia de misiones ya cargada: sin el
     * {@code fetch}, la colección LAZY volvería desligada.
     */
    @Query("SELECT c FROM Categoria c LEFT JOIN FETCH c.categoriaMisiones WHERE c.posicionSecuencia = "
            + "(SELECT MIN(c2.posicionSecuencia) FROM Categoria c2)")
    Optional<Categoria> obtenerCategoriaBase();

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

    // Secuencia de posiciones: los @Modifying usan flushAutomatically para no pisar inserts
    // sin flushear. Sin clearAutomatically, para no desligar la Categoria que se está editando.

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
     * Posiciones ocupadas, ordenadas. Se usa en vez de {@code count()}: con huecos,
     * {@code count()} daría un límite superior mayor que la posición realmente ocupada.
     */
    @Query("SELECT c.posicionSecuencia FROM Categoria c WHERE c.posicionSecuencia IS NOT NULL ORDER BY c.posicionSecuencia ASC")
    List<Integer> listarPosiciones();

    /** Si la posición ya está tomada por otra categoría. */
    boolean existsByPosicionSecuencia(Integer posicionSecuencia);

    /**
     * Categorías que tienen esta misión en su secuencia. Hay que soltar la referencia antes
     * de borrarla: {@code CategoriaMision} es el lado propietario y no tiene cascada.
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
