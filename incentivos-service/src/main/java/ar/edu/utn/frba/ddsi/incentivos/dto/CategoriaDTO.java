package ar.edu.utn.frba.ddsi.incentivos.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CategoriaDTO {
    private String nombre;
    private Integer posicionSecuencia;
    private List<String> misiones;
}
