package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingMesDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorRanking;
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
  private final GestorRanking gestorRanking;

  public RankingService(RepositorioRankings repoRankings, GestorRanking gestorRanking) {
    this.repoRankings = repoRankings;
    this.gestorRanking = gestorRanking;
  }

  public RankingDTO obtenerPuestoRankingActual(UUID idUsuario){
    RankingMensual rank = repoRankings.findFirstByOrderByPeriodoDesc()
            .orElseThrow(InexistenteException::new);

    Ranking puesto = rank.getPosiciones().stream()
            .filter(ranking -> ranking.getIdUsuario().equals(idUsuario))
            .findFirst().orElse(null);

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

  public RankingDTO convertirRankingADTO(Ranking ranking) {
    return new RankingDTO(
        ranking.getNombreUsuario(),
        ranking.getPuesto(),
        ranking.getMisionesCumplidas()
    );
  }

  //para el rankingScheduler
  public void crearRankingMensual(){
    YearMonth periodo = YearMonth.now().minusMonths(1);
    if (repoRankings.findByPeriodo(periodo).isPresent()) {
      throw new IllegalArgumentException("Ya existe un ranking para el período: " + periodo);
    }

    RankingMensual rankingCreado = gestorRanking.generarYPersistirRankingMensual(periodo);

    repoRankings.save(rankingCreado);
  }

  //para pruebas de crear ranking
  @Transactional
  public RankingMesDTO crearRanking(YearMonth periodo) {
    if (repoRankings.findByPeriodo(periodo).isPresent()) {
      throw new IllegalArgumentException("Ya existe un ranking para el período: " + periodo);
    }

    RankingMensual rankingCreado = gestorRanking.generarYPersistirRankingMensual(periodo);

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
}
