package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import java.time.YearMonth;
import java.util.UUID;

/** Encabezado de un ranking publicado, sin cargar sus posiciones. */
public record RankingResumenDTO(UUID idRanking, YearMonth periodo) {
}
