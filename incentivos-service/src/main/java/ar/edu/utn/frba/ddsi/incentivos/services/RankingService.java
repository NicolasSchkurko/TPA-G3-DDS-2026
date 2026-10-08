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
 * Consulta y generación del ranking de colaboradores. Es un snapshot por mes, no un cálculo
 * en vivo: una vez publicado no se toca. Se puntúa por insignias del mes, no por monto.
 */
@Service
public class RankingService {

    /** Posiciones por defecto al consultar el ranking; no limita lo que se persiste. */
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
     * Devuelve las primeras {@code limite} posiciones. El ranking ya viene ordenado, así que
     * solo recorta la lista: el snapshot se persiste completo.
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
     * Camino del scheduler: genera el ranking del mes anterior sin pedir administrador, por
     * eso delega en el método privado que no valida permisos.
     */
    @Transactional
    public RankingMesDTO crearRankingMensualActual() {
        return generarYGuardar(YearMonth.now().minusMonths(1));
    }

    /** Genera el ranking de un período desde un request HTTP. Exige administrador. */
    @Transactional
    public RankingMesDTO crearRankingMensual(UUID idAdmin, YearMonth periodo) {
        validadorAdmin.verificarPermisos(idAdmin);

        if (periodo == null) {
            throw new DatosInvalidosException("El ranking necesita un período");
        }

        return generarYGuardar(periodo);
    }

    /** Borra un ranking publicado. Exige administrador. */
    @Transactional
    public void eliminarRanking(UUID idAdmin, UUID idRanking) {
        validadorAdmin.verificarPermisos(idAdmin);

        if (!repoRankings.existsById(idRanking)) {
            throw new InexistenteException();
        }
        repoRankings.deleteById(idRanking);
    }

    /**
     * El trabajo en sí, sin permisos: lo comparten el endpoint y el scheduler. El chequeo de
     * período va acá para que el scheduler también lo respete.
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
     * Rechaza un período que todavía no cerró: un ranking del mes en curso o futuro rompería
     * las consultas del "actual". Solo se publica un mes cerrado.
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
     * El ranking más reciente publicado. No recalcula nada y solo cuenta períodos cerrados.
     */
    @Transactional(readOnly = true)
    public RankingMesDTO obtenerRankingActual() {
        RankingMensual rank = repoRankings
                .findFirstByPeriodoLessThanOrderByPeriodoDesc(YearMonth.now())
                                                                            .orElseThrow(InexistenteException::new);

        return convertirRankingMesADTO(rank);
    }

    /**
     * Todos los rankings publicados, del más reciente al más viejo. Usa {@code @EntityGraph}
     * para evitar una consulta por ranking.
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
     * Arma el ranking de un mes ya cerrado. El corte se pasa como rango de instantes para que
     * la base use el índice, y se persiste entero: el recorte va al responder.
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
