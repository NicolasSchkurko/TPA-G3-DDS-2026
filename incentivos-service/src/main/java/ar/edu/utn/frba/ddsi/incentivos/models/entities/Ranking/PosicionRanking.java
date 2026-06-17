package ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter

public class PosicionRanking {
    private Integer puesto;
    private UUID idUsuario;
    private UUID idPerfil;
    private String nombreUsuario;
    private Integer misionesCumplidasEnPeriodo;

    public PosicionRanking(Integer puesto,
                           UUID idPerfil,
                           UUID idUsuario,
                           String nombreUsuario,
                           Integer misionesCumplidasEnPeriodo) {
        this.puesto = puesto;
        this.idUsuario = idUsuario;
        this.idPerfil = idPerfil;
        this.nombreUsuario = nombreUsuario;
        this.misionesCumplidasEnPeriodo = misionesCumplidasEnPeriodo;
    }
}
