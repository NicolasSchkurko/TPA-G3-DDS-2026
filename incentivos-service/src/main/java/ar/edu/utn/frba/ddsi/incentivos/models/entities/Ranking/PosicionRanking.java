package ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter

public class PosicionRanking {
    private Integer puesto;
    private Integer misionesCumplidasEnPeriodo;

    public PosicionRanking(Integer puesto) {
        this.puesto = puesto;
        this.misionesCumplidasEnPeriodo = 0;
    }
}
