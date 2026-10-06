package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingMesDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.ValidadorAdmin;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioRankings;
import java.time.YearMonth;
import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta y generación del ranking de colaboradores.
 *
 * <p>El ranking es un snapshot por mes, no un cálculo en vivo: una vez publicado no se
 * vuelve a tocar. Eso es lo que permite que cambiar el criterio de las misiones no altere
 * lo que se vio en meses anteriores.
 *
 * <p>Se puntúa por insignias obtenidas en el mes, no por monto donado: lo que el gamificado
 * busca premiar es la constancia, no la suma.
 */
@Service
public class RankingService {

    /**
     * Cuántas posiciones guarda el snapshot mensual por defecto cuando se consulta el
     * ranking sin pedir un tamaño.
     *
     * <p><b>Esto NO limita lo que se persiste</b>, que es el cambio del punto 2: antes el
     * scheduler guardaba solo 10 y por eso un {@code GET /api/rankings/{id}/top?limite=50}
     * devolvía 10 en silencio, sin avisar de que el ranking estaba truncado. Ahora el
     * snapshot es completo y el {@code limite} se aplica al responder.
     *
     * <p>Es el valor por defecto del parámetro {@code limite} del endpoint, nada más.
     */
    public static final int RANKING_PREDETERMINADO = 10;

    private final RepositorioRankings repoRankings;
    private final RepositorioPerfiles repoPerfiles;
    private final ValidadorAdmin validadorAdmin;

    public RankingService(RepositorioRankings repoRankings,
                                                RepositorioPerfiles repoPerfiles,
                                                ValidadorAdmin validadorAdmin) {
        this.repoRankings = repoRankings;
        this.repoPerfiles = repoPerfiles;
        this.validadorAdmin = validadorAdmin;
    }

    /** El puesto de un donante en el ranking del mes en curso. */
    @Transactional(readOnly = true)
    public RankingDTO obtenerPuestoRankingActual(UUID idUsuario) {

        Ranking puesto = repoRankings.obtenerPosicionActualDeUsuario(idUsuario);

        if (puesto == null) {
            throw new InexistenteException(
                    "El usuario " + idUsuario + " no tiene puesto en el ranking actual"
            );
        }

        return this.convertirRankingADTO(puesto);
    }

    /** Un ranking publicado, con todas sus posiciones. */
    @Transactional(readOnly = true)
    public RankingMesDTO obtenerRanking(UUID idRanking) {
        RankingMensual rank = repoRankings.findById(idRanking)
                                                                            .orElseThrow(InexistenteException::new);

        return convertirRankingMesADTO(rank);
    }

    /**
     * Devuelve las primeras {@code limite} posiciones del ranking.
     *
     * <p>El ranking ya viene ordenado por puesto, así que alcanza con recortar la lista. Y el
     * recorte es <b>aquí</b> y no al generar: como el snapshot se persiste completo (punto
     * 2), un {@code limite} de 50 devuelve las 50 primeras aunque el ranking tenga más. Antes
     * devolvía 10 sin decir nada, porque el snapshot era top 10.
     */
    @Transactional(readOnly = true)
    public RankingMesDTO obtenerRankingConLimite(UUID idRanking, int limite) {
        if (limite <= 0) {
            throw new IllegalArgumentException("El límite debe ser mayor a cero");
        }

        RankingMensual rank = repoRankings.findById(idRanking)
                                                                            .orElseThrow(InexistenteException::new);

        return new RankingMesDTO(
                rank.getIdRanking(),
                rank.getPosiciones().stream()
                        .limit(limite)
                        .map(this::convertirRankingADTO).toList(),
                rank.getPeriodo());
    }

    /**
     * Camino del scheduler: genera el ranking del mes anterior sin pedir administrador.
     *
     * <p>No es un agujero: el scheduler corre dentro del proceso y no hay request ni cliente
     * que pueda invocarlo. Por eso NO delega en {@link #crearRankingMensual} sino en el
     * método privado, que es el que no valida permisos: si delegara, el scheduler tendría que
     * inventar un id de administrador.
     */
    @Transactional
    public RankingMesDTO crearRankingMensualActual() {
        return generarYGuardar(YearMonth.now().minusMonths(1));
    }

    /**
     * Genera el ranking de un período, desde un request HTTP.
     *
     * <p><b>Exige administrador</b> (punto 21). Antes no lo pedía y cualquiera que llegara
     * al servicio podía generar rankings para meses históricos arbitrarios. Es un problema
     * distinto del punto 1: allá el admin se valida contra un header que elige el cliente,
     * acá directamente no había validación de ningún tipo.
     */
    @Transactional
    public RankingMesDTO crearRankingMensual(UUID idAdmin, YearMonth periodo) {
        validadorAdmin.verificarPermisos(idAdmin);

        if (periodo == null) {
            throw new DatosInvalidosException("El ranking necesita un período");
        }

        return generarYGuardar(periodo);
    }

    /**
     * Borra un ranking publicado.
     *
     * <p><b>Exige administrador</b> (punto 21), por lo mismo que crear: borrar un ranking ya
     * publicado es una escritura de administración, no una consulta.
     */
    @Transactional
    public void eliminarRanking(UUID idAdmin, UUID idRanking) {
        validadorAdmin.verificarPermisos(idAdmin);

        if (!repoRankings.existsById(idRanking)) {
            throw new InexistenteException();
        }
        repoRankings.deleteById(idRanking);
    }

    /**
     * El trabajo en sí, sin permisos: lo comparten el endpoint y el scheduler.
     *
     * <p>El chequeo de período va acá y no solo en {@link #crearRankingMensual} porque el
     * scheduler entra por el mismo camino y también tiene que respetarlo.
     */
    private RankingMesDTO generarYGuardar(YearMonth periodo) {
        verificarPeriodoCerrado(periodo);

        if (repoRankings.findByPeriodo(periodo).isPresent()) {
            throw new IllegalArgumentException("Ya existe un ranking para el período: " + periodo);
        }

        RankingMensual rankingCreado = generarRankingMensual(periodo);
        repoRankings.save(rankingCreado);

        return convertirRankingMesADTO(rankingCreado);
    }

    /**
     * Rechaza un período que todavía no cerró (punto 32).
     *
     * <p>Es un 400 y no un "no hay datos, te devuelvo vacío" porque un ranking vacío no es
     * un resultado: es un ranking que rompe las consultas del "actual". Como
     * {@code obtenerRankingActual} tomaba el período más alto existente, guardar uno del mes
     * en curso o de un mes futuro hacía que {@code GET /api/rankings/actual} devolviera la
     * lista vacía y {@code puestoRanking} respondiera 404 para todos los usuarios, aunque
     * el ranking verdadero estuviera ahí. Quedaba roto hasta que alguien borrara a mano el
     * ranking futuro.
     *
     * <p>La comparación es contra el mes en curso, no contra hoy: un ranking de este mes
     * tampoco sirve, porque el mes todavía no terminó y siempre sale incompleto. Lo único
     * publicable es un mes cerrado, que es justo lo que genera el scheduler.
     */
    private void verificarPeriodoCerrado(YearMonth periodo) {
        YearMonth enCurso = YearMonth.now();

        if (!periodo.isBefore(enCurso)) {
            throw new IllegalArgumentException(
                    "No se puede generar el ranking de " + periodo + " porque ese mes todavía no "
                            + "terminó. Solo se publica un mes ya cerrado.");
        }
    }

    /**
     * El ranking más reciente que se publicó.
     *
     * <p>No recalcula nada, y solo cuenta períodos ya cerrados: si el mes en curso todavía no
     * cerró, devuelve el del mes anterior (punto 32).
     */
    @Transactional(readOnly = true)
    public RankingMesDTO obtenerRankingActual() {
        RankingMensual rank = repoRankings
                .findFirstByPeriodoLessThanOrderByPeriodoDesc(YearMonth.now())
                                                                            .orElseThrow(InexistenteException::new);

        return convertirRankingMesADTO(rank);
    }

    /**
     * Todos los rankings publicados, del más reciente al más viejo.
     *
     * <p>Usa el método con {@code @EntityGraph} y no el {@code findAll} de JpaRepository: el
     * {@code fetch} de las posiciones es lo que evita una consulta por ranking (punto 22).
     */
    @Transactional(readOnly = true)
    public Page<RankingMesDTO> obtenerHistorialRankings(Pageable pageable) {
        return repoRankings.findAllByOrderByPeriodoDesc(pageable).map(this::convertirRankingMesADTO);
    }

    private RankingMesDTO convertirRankingMesADTO(RankingMensual ranking) {
        return new RankingMesDTO(
                ranking.getIdRanking(),
                ranking.getPosiciones().stream()
               .map(this::convertirRankingADTO)
               .toList(),
                ranking.getPeriodo()
        );
    }

    /**
     * Arma el ranking de un mes ya cerrado.
     *
     * <p>El corte del mes se pasa como <b>rango de instantes</b> y no como "mes y año" (punto
     * 22): la base resuelve el filtro con un índice en {@code fecha_obtencion} en vez de
     * tener que evaluar {@code MONTH()} y {@code YEAR()} sobre cada fila de la tabla.
     *
     * <p><b>Se persiste el ranking entero (punto 2).</b> Antes se pasaba
     * {@code PageRequest.of(0, RANKING_PREDETERMINADO)}, o sea top 10, y como el snapshot
     * guardaba lo que viniera, pedir el podio con {@code limite=50} devolvía 10 en silencio.
     * El corte va ahora al responder, en {@link #obtenerRankingConLimite}.
     *
     * <p><b>El precio, dicho claro:</b> la tabla de posiciones crece con todos los
     * donantes que obtuvieron al menos una insignia en el mes, no con diez. Es lo que
     * corresponde: un ranking publicado es un hecho del período, y recortarlo es perder
     * información que el cliente pidió explícitamente. Si algún día el volumen lo hace
     * inviable, la solución es generar el snapshot por bloques, no volver a truncarlo en
     * silencio.
     */
    private RankingMensual generarRankingMensual(YearMonth periodo) {
        LocalDateTime inicioDelPeriodo = periodo.atDay(1).atStartOfDay();
        LocalDateTime inicioDelPeriodoSiguiente = periodo.plusMonths(1).atDay(1).atStartOfDay();

        List<Object[]> todosLosPerfilesDelPeriodo = repoPerfiles.calcularRankingMensual(
                inicioDelPeriodo,
                inicioDelPeriodoSiguiente,
                Pageable.unpaged()
        );

        RankingMensual rankingDelMes = new RankingMensual(periodo);

        rankingDelMes.calcularYAgregarPosiciones(todosLosPerfilesDelPeriodo);

        return rankingDelMes;
    }

    private RankingDTO convertirRankingADTO(Ranking ranking) {
        return new RankingDTO(
                ranking.getNombreUsuario(),
                ranking.getPuesto(),
                ranking.getMisionesCumplidas()
        );
    }
}
