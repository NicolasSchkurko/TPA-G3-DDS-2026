package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InsigniaDTO {
    private String nombre;
    private String descripcion;
    private String urlImagen;

    public InsigniaDTO(String nombre, String descripcion, String urlImagen) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.urlImagen = urlImagen;
    }
}