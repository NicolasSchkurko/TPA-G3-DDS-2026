package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import java.util.UUID;

/** Una fila agregada del ranking mensual: donante y cantidad de insignias del período. */
public record RankingFilaDTO(UUID idUsuario, String nombreUsuario, Long totalMisiones) {
}
