package ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter

public class PosicionRanking {
    private Integer puesto;
    private UUID idUsuario;
    private String nombreUsuario;
    private Integer misionesCumplidasEnPeriodo;

    public PosicionRanking(UUID idUsuario, String nombreUsuario, Integer misionesDelMes) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.misionesCumplidasEnPeriodo = misionesDelMes;
    }

    public PosicionRanking(Integer puesto, UUID idUsuario, String nombreUsuario, Integer misionesCumplidasEnPeriodo) {
        this.puesto = puesto;
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.misionesCumplidasEnPeriodo = misionesCumplidasEnPeriodo;
    }
}
