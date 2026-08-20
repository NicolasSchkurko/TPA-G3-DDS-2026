package ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Ranking {
    private PosicionRanking posicionRanking;
    private UUID idUsuario;
    private UUID idPerfil;
    private String nombreUsuario;

    public Ranking(PosicionRanking  posicionRanking,
                   UUID idUsuario,
                   UUID idPerfil) {
        this.posicionRanking = posicionRanking;
        this.idUsuario = idUsuario;
        this.idPerfil = idPerfil;
    }
}
