package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingMesDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioRankings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RankingService {

  private final RepositorioRankings repoRankings;
  private final RepositorioPerfiles repoPerfiles;

  public RankingService(RepositorioRankings repoRankings, RepositorioPerfiles repoPerfiles) {
    this.repoRankings = repoRankings;
    this.repoPerfiles = repoPerfiles;
  }

  public RankingDTO obtenerPuestoRankingActual(UUID idUsuario) {

    Ranking puesto = repoRankings.obtenerPosicionActualDeUsuario(idUsuario);

    return puesto != null ? this.convertirRankingADTO(puesto) : null;
  }

  public RankingMesDTO obtenerRanking(UUID idRanking) {
    RankingMensual rank = repoRankings.findById(idRanking)
                                      .orElseThrow(InexistenteException::new);

    return convertirRankingMesADTO(rank);
  }

  public RankingMesDTO obtenerTop3Ranking(UUID idRanking) {
    RankingMensual rank = repoRankings.findById(idRanking)
                                      .orElseThrow(InexistenteException::new);

    return new RankingMesDTO(
        rank.getIdRanking(),
        rank.getPosiciones().stream()
            .limit(3)
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

  public RankingMesDTO obtenerRankingActual() {
    RankingMensual rank = repoRankings.findFirstByOrderByPeriodoDesc()
                                      .orElseThrow(InexistenteException::new);

    return convertirRankingMesADTO(rank);
  }

  public List<RankingMesDTO> obtenerHistorialRankings() {
    List<RankingMensual> rankings = repoRankings.findAll();

    return rankings.stream()
                   .map(this::convertirRankingMesADTO)
                   .collect(Collectors.toList());
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

    List<Object[]> topPerfiles = repoPerfiles.calcularRankingMensual(mes, anio);

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