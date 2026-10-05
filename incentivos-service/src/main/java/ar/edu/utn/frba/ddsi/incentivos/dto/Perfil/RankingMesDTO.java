package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RankingMesDTO {
    private UUID idRanking;
    private YearMonth periodo;
    private List<RankingDTO> ranking;

    public RankingMesDTO(UUID idRanking, List<RankingDTO> ranking, YearMonth periodo) {
        this.idRanking = idRanking;
        this.periodo = periodo;
        this.ranking = ranking;
    }
}
