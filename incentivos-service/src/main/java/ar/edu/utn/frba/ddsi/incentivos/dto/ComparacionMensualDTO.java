package ar.edu.utn.frba.ddsi.incentivos.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.YearMonth;

@Getter
@Setter

public class ComparacionMensualDTO {
    private YearMonth periodo;
    //tipos de cmps x mes
    private Integer cantidadDonaciones;
    private Integer organizacionesAyudadas;
    private Integer bienesDonados;
}
