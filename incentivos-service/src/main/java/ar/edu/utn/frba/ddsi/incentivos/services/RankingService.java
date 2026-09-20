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

  public RankingMesDTO obtenerRanking(UUID idRanking) {
    RankingMensual rank = repoRankings.findById(idRanking)
                                      .orElseThrow(InexistenteException::new);

    return new RankingMesDTO(
        rank.getPosiciones().stream()
            .map(this::convertirRankingADTO).toList(),
        rank.getPeriodo());
  }

  public RankingMesDTO obtenerTop3Ranking(UUID idRanking) {
    RankingMensual rank = repoRankings.findById(idRanking)
                                      .orElseThrow(InexistenteException::new);

    return new RankingMesDTO(
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

  @Transactional
  public RankingMesDTO crearRanking(YearMonth periodo) {
    if (repoRankings.findByPeriodo(periodo).isPresent()) {
      throw new IllegalArgumentException("Ya existe un ranking para el período: " + periodo);
    }

    RankingMensual rankingCreado = gestorRanking.generarYPersistirRankingMensual(periodo);

    return new RankingMesDTO(
        rankingCreado.getPosiciones().stream()
                     .map(this::convertirRankingADTO).toList(),
        rankingCreado.getPeriodo()
    );
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
    YearMonth mesActual = YearMonth.now();
    RankingMensual rank = repoRankings.findByPeriodo(mesActual)
                                      .orElseThrow(InexistenteException::new);

    return new RankingMesDTO(
        rank.getPosiciones().stream()
            .map(this::convertirRankingADTO).toList(),
        rank.getPeriodo());
  }

  public List<RankingMesDTO> obtenerHistorialRankings() {
    List<RankingMensual> rankings = repoRankings.findAll();

    return rankings.stream()
        .map(rank -> new RankingMesDTO(
            rank.getPosiciones().stream()
                .map(this::convertirRankingADTO).toList(),
            rank.getPeriodo()))
        .collect(Collectors.toList());
  }
}