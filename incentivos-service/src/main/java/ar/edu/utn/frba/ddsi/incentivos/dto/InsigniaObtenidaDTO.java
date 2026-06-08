package ar.edu.utn.frba.ddsi.incentivos.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter

public class InsigniaObtenidaDTO {
    //creo q es la forma intermedia para q se le asigne una fecha
    //de obtencion a la insignia segun el usuario
    private InsigniaDTO insignia;
    private LocalDate fechaObtencion;
}
