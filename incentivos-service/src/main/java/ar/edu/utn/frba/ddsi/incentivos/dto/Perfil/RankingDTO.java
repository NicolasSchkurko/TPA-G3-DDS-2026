package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RankingDTO {
    private String nombreUsuario;

    // Dejó de llamarse "posicionRanking" porque esa clase ya no existe.
    private Integer puesto;

    // Pasa a ser Long porque el COUNT() de la base de datos devuelve un Long.
    private Long misionesCumplidas;

    public RankingDTO() {}

    public RankingDTO(String nombreUsuario, Integer puesto, Long misionesCumplidas) {
        this.nombreUsuario = nombreUsuario;
        this.puesto = puesto;
        this.misionesCumplidas = misionesCumplidas;
    }
}