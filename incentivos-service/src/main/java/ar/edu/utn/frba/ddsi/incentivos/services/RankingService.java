package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingFilaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingMesDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingResumenDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.ConflictoException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioRankings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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

    /** Tope del parámetro {@code limite} del top para no volver a cargar todo el snapshot. */
    public static final int LIMITE_MAXIMO = 100;

    /** Filas por página durante la generación del snapshot. */
    private static final int TAMANO_BLOQUE_RANKING = 500;

    private final RepositorioRankings repoRankings;
    private final RepositorioPerfiles repoPerfiles;

    @PersistenceContext
    private EntityManager entityManager;

    public RankingService(RepositorioRankings repoRankings,
                          RepositorioPerfiles repoPerfiles
    ) {
        this.repoRankings = repoRankings;
        this.repoPerfiles = repoPerfiles;
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

    /** Una página de un ranking publicado. */
    @Transactional(readOnly = true)
    public RankingMesDTO obtenerRanking(UUID idRanking, Pageable pageable) {
        RankingMensual rank = repoRankings.findById(idRanking)
                .orElseThrow(InexistenteException::new);

        return convertirRankingMesADTO(
                rank,
                repoRankings.paginaPosiciones(idRanking, pageable)
        );
    }

    /** Devuelve las primeras {@code limite} posiciones, con el límite aplicado en la base. */
    @Transactional(readOnly = true)
    public RankingMesDTO obtenerRankingConLimite(UUID idRanking, int limite) {
        if (limite <= 0) {
            throw new IllegalArgumentException("El límite debe ser mayor a cero");
        }

        int limiteEfectivo = Math.min(limite, LIMITE_MAXIMO);
        RankingMensual rank = repoRankings.findById(idRanking)
                .orElseThrow(InexistenteException::new);

        return convertirRankingMesADTO(
                rank,
                repoRankings.paginaPosiciones(idRanking, PageRequest.of(0, limiteEfectivo))
        );
    }

    /**
     * Camino del scheduler: genera el ranking del mes anterior sin pedir administrador, por
     * eso delega en el método privado que no valida permisos.
     */
    @Transactional
    public RankingMesDTO crearRankingMensualActual() {
        YearMonth periodo = YearMonth.now().minusMonths(1);
        return repoRankings.findByPeriodo(periodo)
                .map(this::convertirRankingMesADTO)
                .orElseGet(() -> generarYGuardar(periodo));
    }

    /** Genera el ranking de un período desde un request HTTP. Exige administrador. */
    @Transactional
    public RankingMesDTO crearRankingMensual(UUID idAdmin, YearMonth periodo) {

        if (periodo == null) {
            throw new DatosInvalidosException("El ranking necesita un período");
        }

        return generarYGuardar(periodo);
    }

    /** Borra un ranking publicado. Exige administrador. */
    @Transactional
    public void eliminarRanking(UUID idAdmin, UUID idRanking) {

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
            throw new ConflictoException("Ya existe un ranking para el período: " + periodo);
        }

        RankingMensual rankingCreado = generarRankingMensual(periodo);

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
    public RankingMesDTO obtenerRankingActual(Pageable pageable) {
        RankingMensual rank = repoRankings
                .findFirstByPeriodoLessThanOrderByPeriodoDesc(YearMonth.now())
                .orElseThrow(InexistenteException::new);

        return convertirRankingMesADTO(
                rank,
                repoRankings.paginaPosiciones(rank.getIdRanking(), pageable)
        );
    }

    /**
     * Encabezados de todos los rankings publicados, del más reciente al más viejo. No carga
     * las posiciones: cada ranking se consulta aparte y paginado.
     */
    @Transactional(readOnly = true)
    public Page<RankingResumenDTO> obtenerHistorialRankings(Pageable pageable) {
        return repoRankings.findAllByOrderByPeriodoDesc(pageable)
                .map(ranking -> new RankingResumenDTO(
                        ranking.getIdRanking(), ranking.getPeriodo()));
    }

    private RankingMesDTO convertirRankingMesADTO(RankingMensual ranking) {
        return convertirRankingMesADTO(
                ranking,
                repoRankings.paginaPosiciones(
                        ranking.getIdRanking(),
                        PageRequest.of(0, RANKING_PREDETERMINADO))
        );
    }

    private RankingMesDTO convertirRankingMesADTO(RankingMensual ranking, Page<Ranking> posiciones) {
        return new RankingMesDTO(
                ranking.getIdRanking(),
                posiciones.map(this::convertirRankingADTO),
                ranking.getPeriodo()
        );
    }

    /**
     * Arma el ranking de un mes ya cerrado por páginas: proyecta solo donante y total, crea
     * las posiciones de cada página y limpia el contexto de persistencia antes de la
     * siguiente, para no retener todo el ranking en memoria.
     */
    private RankingMensual generarRankingMensual(YearMonth periodo) {
        LocalDateTime inicioDelPeriodo = periodo.atDay(1).atStartOfDay();
        LocalDateTime inicioDelPeriodoSiguiente = periodo.plusMonths(1).atDay(1).atStartOfDay();

        RankingMensual rankingDelMes = new RankingMensual(periodo);
        repoRankings.saveAndFlush(rankingDelMes);
        UUID idRanking = rankingDelMes.getIdRanking();

        int puestoActual = 1;
        int indiceGeneral = 1;
        Long misionesPrevias = -1L;
        Pageable pagina = PageRequest.of(0, TAMANO_BLOQUE_RANKING);

        while (true) {
            Slice<RankingFilaDTO> filas = repoPerfiles.calcularRankingMensual(
                    inicioDelPeriodo,
                    inicioDelPeriodoSiguiente,
                    pagina
            );

            if (filas.isEmpty()) {
                break;
            }

            RankingMensual referencia =
                    entityManager.getReference(RankingMensual.class, idRanking);
            for (RankingFilaDTO fila : filas) {
                long total = fila.totalMisiones();
                if (total != misionesPrevias) {
                    puestoActual = indiceGeneral;
                }
                entityManager.persist(new Ranking(
                        referencia,
                        fila.idUsuario(),
                        fila.nombreUsuario(),
                        puestoActual,
                        total
                ));
                misionesPrevias = total;
                indiceGeneral++;
            }

            entityManager.flush();
            entityManager.clear();

            if (!filas.hasNext()) {
                break;
            }
            pagina = pagina.next();
        }

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
