package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingMesDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioRankings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class RankingService {

  /**
   * Cantidad de posiciones que se persisten al snapshot mensual cuando el
   * scheduler genera el ranking de forma automática.
   */
  public static final int RANKING_PREDETERMINADO = 10;

  private final RepositorioRankings repoRankings;
  private final RepositorioPerfiles repoPerfiles;

  public RankingService(RepositorioRankings repoRankings, RepositorioPerfiles repoPerfiles) {
    this.repoRankings = repoRankings;
    this.repoPerfiles = repoPerfiles;
  }

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

  @Transactional(readOnly = true)
  public RankingMesDTO obtenerRanking(UUID idRanking) {
    RankingMensual rank = repoRankings.findById(idRanking)
                                      .orElseThrow(InexistenteException::new);

    return convertirRankingMesADTO(rank);
  }

  /**
   * Devuelve las primeras {@code limite} posiciones del ranking. El ranking ya viene
   * ordenado por puesto, por lo que alcanza con recortar la lista.
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

  @Transactional
  public RankingMesDTO crearRankingMensualActual(){
    YearMonth periodo = YearMonth.now().minusMonths(1);
    return this.crearRankingMensual(periodo);
  }

  @Transactional
  public RankingMesDTO crearRankingMensual(YearMonth periodo) {
    if (periodo == null) {
      throw new DatosInvalidosException("El ranking necesita un período");
    }

    if (repoRankings.findByPeriodo(periodo).isPresent()) {
      throw new IllegalArgumentException("Ya existe un ranking para el período: " + periodo);
    }

    RankingMensual rankingCreado = generarRankingMensual(periodo);

    repoRankings.save(rankingCreado);

    return convertirRankingMesADTO(rankingCreado);
  }

  @Transactional
  public Boolean eliminarRanking(UUID idRanking) {
    if (!repoRankings.existsById(idRanking)) {
      throw new InexistenteException();
    }
    repoRankings.deleteById(idRanking);
    return true;
  }

  @Transactional(readOnly = true)
  public RankingMesDTO obtenerRankingActual() {
    RankingMensual rank = repoRankings.findFirstByOrderByPeriodoDesc()
                                      .orElseThrow(InexistenteException::new);

    return convertirRankingMesADTO(rank);
  }

  @Transactional(readOnly = true)
  public Page<RankingMesDTO> obtenerHistorialRankings(Pageable pageable) {
    return repoRankings.findAll(pageable).map(this::convertirRankingMesADTO);
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

  private RankingMensual generarRankingMensual(YearMonth periodo) {
    int mes = periodo.getMonthValue();
    int anio = periodo.getYear();

    List<Object[]> topPerfiles = repoPerfiles.calcularRankingMensual(
        mes,
        anio,
        PageRequest.of(0, RANKING_PREDETERMINADO)
    );

    RankingMensual rankingDelMes = new RankingMensual(periodo);

    rankingDelMes.calcularYAgregarPosiciones(topPerfiles);

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
