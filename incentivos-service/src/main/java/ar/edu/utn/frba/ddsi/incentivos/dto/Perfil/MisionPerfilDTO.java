package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MisionPerfilDTO {
    private String nombreMision;
    private String descripcion;
    private String insigniaObjetivo;

    public MisionPerfilDTO(String nomM, String nomI, String descripcion){
        this.descripcion = descripcion;
        this.insigniaObjetivo = nomI;
        this.nombreMision = nomM;
    }
}
