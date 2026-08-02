package ar.edu.utn.frba.ddsi.incentivos.dto.Admin;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CategoriaDTO {
    private String nombre;
    private Integer posicionSecuencia;
    private List<String> misiones;

    public CategoriaDTO(String nombre,
                        Integer posicionSecuencia,
                        List<String> misiones){
        this.nombre = nombre;
        this.posicionSecuencia = posicionSecuencia;
        this.misiones = misiones;
    }
}
