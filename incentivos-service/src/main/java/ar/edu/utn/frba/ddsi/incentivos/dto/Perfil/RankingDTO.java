package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class RankingDTO {
    private String nombreUsuario;
    private Integer posicionRanking;
    private Integer cantidadMisionesCompletas;
}
