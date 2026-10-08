package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RankingDTO {
    private String nombreUsuario;

    private Integer puesto;

    // Long porque el COUNT() de la base devuelve Long.
    private Long misionesCumplidas;

    public RankingDTO() {}

    public RankingDTO(String nombreUsuario, Integer puesto, Long misionesCumplidas) {
        this.nombreUsuario = nombreUsuario;
        this.puesto = puesto;
        this.misionesCumplidas = misionesCumplidas;
    }
}