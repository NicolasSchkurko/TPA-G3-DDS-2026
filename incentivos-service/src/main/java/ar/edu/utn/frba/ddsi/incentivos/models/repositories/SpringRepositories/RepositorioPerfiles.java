package ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.InsigniaObtenida;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Consultas sobre los perfiles de donante.
 *
 * <p>Incluye los tres {@code countBy...} que usa el borrado en cascada para decidir si es
 * seguro: sin ellos, borrar una categoría con donantes reventaba por violación de FK y el
 * cliente recibía un 500 opaco (punto 18).
 */
@Repository
public interface RepositorioPerfiles extends JpaRepository<Perfil, UUID> {

    /**
 * El perfil de un donante, con lo que necesita para armarse el DTO.
 *
 * <p>El {@code @EntityGraph} es necesario, no decorativo (punto 22): {@code PerfilDTO} se arma leyendo
 * las insignias y de cada una el nombre de la insignia conseguida. Como
 * {@code insigniasObtenidas} es LAZY y ahora {@code InsigniaObtenida.insignia} también, sin
 * el {@code fetch} eso son dos consultas por donante, y si el mapeo llegara a correr fuera
 * de la transacción sería un {@code LazyInitializationException} (punto 6).
 *
 * <p>La diferencia con la paginación de insignias está en que ahí se trae la colección; acá
 * además hay que traer la de dentro de cada una, porque el DTO solo quiere los nombres.
 */
@EntityGraph(attributePaths = {
        "categoriaActual",
        "insigniasObtenidas",
        "insigniasObtenidas.insignia",
        "progresoMisionActual.mision"
})
    Optional<Perfil> findByIdUsuario(UUID idUsuario);

    /**
     * Progreso de la misión vigente de un donante. Devuelve el {@link ProgresoMision}
     * entero y no solo la {@link Mision} porque el donante necesita ver cuanto
     * lleva recorrido, que solo esta en el progreso.
     */
    @Query("SELECT pm FROM Perfil p JOIN p.progresoMisionActual pm WHERE p.idUsuario = :idUsuario")
    Optional<ProgresoMision> obtenerProgresoMisionPorIdUsuario(@Param("idUsuario") UUID idUsuario);

    boolean existsByIdUsuario(UUID idUsuario);

    // ===== Conteos para decidir si un borrado es seguro (punto 18) =====
    // Sin esto, borrar una categoría con donantes o una misión ya completada revienta por
    // violación de FK y el cliente recibe un 500 opaco. Con esto se puede contestarle un
    // 409 diciendo exactamente cuántas referencias lo bloquean.

    /** Donantes que tienen esta categoría como categoría actual. */
    long countByCategoriaActual(Categoria categoria);

    /** Donantes que están actualmente haciendo esta misión. */
    long countByProgresoMisionActualMision(Mision mision);

    /** Donantes que ya obtuvieron la insignia de la misión. */
    long countByInsigniasObtenidasInsignia(Insignia insignia);

    void deleteByIdUsuario(UUID idUsuario);

    List<Perfil> findAllByCategoriaActual(Categoria categoria);

    /**
     * Donantes cuya mision vigente es la indicada.
     *
     * <p><b>Le faltaba el {@code FROM}.</b> Era {@code "SELECT p JOIN p.progresoMisionActual pm ..."},
     * que no es JPQL valido, y Spring Data lo rechaza al construir el repositorio: el mensaje
     * es un {@code Validation failed for query} que no dice que falta el FROM. Como el bean
     * del repositorio no se podia crear, <b>el servicio entero no arrancaba</b> aunque los 302
     * testsaran en verde — los tests mockean el repositorio, asi que la query nunca se valida.
     */
    @Query("SELECT p "
            + "FROM Perfil p "
            + "JOIN p.progresoMisionActual pm "
            + "WHERE pm.mision.idMision = :idMision")
    List<Perfil> findAllByMisionActual(@Param("idMision") UUID idMision);

    /**
 * Los perfiles cuya misión vigente exige constancia, con la misión y su regla ya cargadas
 * (punto 22).
 *
 * <p>El {@code JOIN} sin {@code FETCH} dejaba que Hibernate se trajera solo el perfil y
 * dejara los proxies de {@code progresoMisionActual} y de {@code progresoMisionActual.mision}
 * para después. Como el scheduler recorre la lista entera y de cada perfil lee la misión
 * para preguntar sus donaciones, eso no era un N+1 hipotético: eran <b>dos consultas más
 * por cada perfil</b>, además de la de sus donaciones. Con 10.000 perfiles, 30.001
 * consultas en una pasada nocturna.
 *
 * <p>El {@code FETCH} deja todo en una sola consulta por bloque. No se puede traer
 * también las donaciones en la misma, porque cada perfil necesita las de <em>su</em> misión
 * y una consulta con las dos colecciones cruzadas da un producto cartesiano; por eso las
 * donaciones siguen yendo aparte, una consulta por perfil.
 */
@Query("SELECT p "
        + "FROM Perfil p "
        + "JOIN FETCH p.progresoMisionActual pm "
        + "JOIN FETCH pm.mision m "
        + "JOIN FETCH m.reglaDeProgreso "
        + "WHERE m.reglaDeProgreso.constancia IS NOT NULL")
    Page<Perfil> buscarPerfilesConMisionQueRequiereConstancia(Pageable pageable);

    /**
 * Pagina las insignias de un perfil, con la insignia traída en la misma consulta.
 *
 * <p>Devuelve {@link InsigniaObtenida} y no la insignia pelada para que se pueda ordenar por
 * fecha de obtencion, que solo existe en la entidad intermedia.
 *
 * <p>El {@code fetch join} de {@code insignia} es lo que evita el N+1 (punto 22): el service
 * convierte cada elemento con {@code obtenida.getInsignia().getNombre()}, y como la relación
 * es LAZY eso sería una consulta por fila de la página. Con el {@code fetch} es una sola. El
 * {@code countQuery} va aparte justamente porque la consulta con {@code fetch} devuelve
 * filas duplicadas y {@code COUNT(DISTINCT)} sobre ella daría el número de joins, no el de
 * insignias.
 */
@Query(value = "SELECT io FROM Perfil p JOIN p.insigniasObtenidas io "
        + "WHERE p.idUsuario = :idUsuario",
        countQuery = "SELECT COUNT(io) FROM Perfil p JOIN p.insigniasObtenidas io "
                + "WHERE p.idUsuario = :idUsuario")
    @EntityGraph(attributePaths = "insignia")
    Page<InsigniaObtenida> paginaInsigniasPorIdUsuario(
            @Param("idUsuario") UUID idUsuario,
            Pageable pageable
    );

    /**
     * Ranking del periodo: cuenta insignias obtenidas dentro del mes.
     *
     * <p>El corte de filas lo aplica el {@link Pageable} recibido, no la query, para que el
     * limite se pueda pedir por parametro.
     *
     * <p><b>El filtro es un rango, no {@code MONTH(fecha) = n AND YEAR(fecha) = a}</b>
     * (punto 22). Aplicar funciones sobre la columna en el {@code WHERE} la vuelve no
     * sargable: con un índice en {@code (fecha_obtencion)} la base no lo puede usar, porque
     * tiene que evaluar la función sobre cada fila antes de comparar. El resultado era un
     * full scan de {@code insignias_obtenidas} cada vez que se generaba un ranking, y el
     * ranking se genera todos los meses. Con el rango {@code >= inicio AND < fin} la
     * comparación es directa y el índice sirve.
     *
     * <p>El rango es semiabierto a propósito: {@code < fin} y no {@code <= fin}. Con
     * {@code <=} habría que sumar un instante al último día del mes, y el error de un día
     * corido al final del período es justo el que hace que un mes aparezca con una
     * infracción de más.
     *
     * @param inicio primer instante del mes, inclusive
     * @param fin   primer instante del mes siguiente, exclusive
     */
    @Query("SELECT p, COUNT(io) as total "
            + "FROM Perfil p JOIN p.insigniasObtenidas io "
            + "WHERE io.fechaObtencion >= :inicio AND io.fechaObtencion < :fin "
            + "GROUP BY p "
            + "ORDER BY total DESC, p.nombreUsuario ASC")
    List<Object[]> calcularRankingMensual(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            Pageable pageable
    );
}
