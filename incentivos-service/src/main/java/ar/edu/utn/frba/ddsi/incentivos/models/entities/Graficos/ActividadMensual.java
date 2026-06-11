package ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos;

import java.time.YearMonth;

import lombok.Getter;
import lombok.Setter;
import ar.edu.utn.frba.ddsi.incentivos.dto.ActividadMensualDTO;

@Getter
@Setter
public class ActividadMensual {
    private YearMonth periodo;
    private Integer cantidadDonaciones;
    private Integer organizacionesAyudadas;

    public ActividadMensualDTO toDTO(){
        ActividadMensualDTO dto = new ActividadMensualDTO();
        dto.setPeriodo(this.periodo);
        dto.setCantidadDonaciones(this.cantidadDonaciones);
        dto.setOrganizacionesAyudadas(this.organizacionesAyudadas);
        return dto;
    }
}
