package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import lombok.Getter;
import lombok.Setter;

import java.time.YearMonth;
import java.util.List;

@Getter
@Setter
public class RankingMesDTO {
    private YearMonth periodo;
    private List<RankingDTO> ranking;

    public RankingMesDTO(List<RankingDTO> ranking, YearMonth periodo){
        this.periodo = periodo;
        this.ranking = ranking;
    }
}
