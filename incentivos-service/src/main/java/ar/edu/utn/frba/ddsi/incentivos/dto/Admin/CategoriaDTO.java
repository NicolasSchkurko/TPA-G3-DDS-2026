package ar.edu.utn.frba.ddsi.incentivos.dto.Admin;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CategoriaDTO {
    private String nombre;
    private Integer posicionSecuencia;
    private List<UUID> misiones;

    public CategoriaDTO(String nombre,
                        Integer posicionSecuencia,
                        List<UUID> misiones){
        this.nombre = nombre;
        this.posicionSecuencia = posicionSecuencia;
        this.misiones = misiones;
    }
}
