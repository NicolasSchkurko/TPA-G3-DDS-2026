package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import java.time.YearMonth;
import java.util.UUID;

/** Encabezado de un ranking publicado, con el total de participantes y sin cargar sus posiciones. */
public record RankingResumenDTO(UUID idRanking, YearMonth periodo, long participantes) {
}
