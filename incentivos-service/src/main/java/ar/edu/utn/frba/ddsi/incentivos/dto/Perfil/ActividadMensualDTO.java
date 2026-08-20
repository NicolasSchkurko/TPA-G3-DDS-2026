package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import lombok.Getter;
import lombok.Setter;

import java.time.YearMonth;

@Getter
@Setter

public class ActividadMensualDTO {
    private YearMonth periodo;
    private Integer cantidadDonaciones;
    private Integer organizacionesAyudadas;

    public ActividadMensualDTO(YearMonth periodo, Integer cantidadDonaciones, Integer organizacionesAyudadas){
        this.periodo = periodo;
        this.cantidadDonaciones = cantidadDonaciones;
        this.organizacionesAyudadas = organizacionesAyudadas;
    }
}