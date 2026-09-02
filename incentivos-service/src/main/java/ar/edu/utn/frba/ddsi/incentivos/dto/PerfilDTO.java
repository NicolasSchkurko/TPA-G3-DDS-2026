package ar.edu.utn.frba.ddsi.incentivos.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public class PerfilDTO {
    private String nombreUsuario;
    private String categoriaActual;
    private List<String> insignias;
    private String misionActual;
    private Integer posicionRanking;
    private String rol;

    public PerfilDTO(String nombreUsuario,
                     String categoriaActual,
                     List<String> insignias,
                     String misionActual,
                     Integer posicionRanking,
                     String rol) {
        this.nombreUsuario = nombreUsuario;
        this.categoriaActual = categoriaActual;
        this.insignias = insignias;
        this.misionActual = misionActual;
        this.posicionRanking = posicionRanking;
        this.rol = rol;
    }
}
