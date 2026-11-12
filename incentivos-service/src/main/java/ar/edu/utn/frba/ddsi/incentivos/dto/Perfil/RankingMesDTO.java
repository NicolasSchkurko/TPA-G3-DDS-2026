package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import java.time.YearMonth;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

@Getter
@Setter
public class RankingMesDTO {
    private UUID idRanking;
    private YearMonth periodo;
    private Page<RankingDTO> ranking;

    public RankingMesDTO(UUID idRanking, Page<RankingDTO> ranking, YearMonth periodo) {
        this.idRanking = idRanking;
        this.periodo = periodo;
        this.ranking = ranking;
    }
}
