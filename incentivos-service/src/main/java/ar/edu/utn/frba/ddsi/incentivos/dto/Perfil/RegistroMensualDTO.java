package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import lombok.Getter;
import lombok.Setter;

import java.time.YearMonth;

@Getter
@Setter
public class RegistroMensualDTO {
    private YearMonth periodo;
    private Integer cantidadDonaciones;
    private Integer cantidadOrganizacionesAyudadas;

    public RegistroMensualDTO(YearMonth periodo, Integer cantidadDonaciones,
                              Integer cantidadOrganizacionesAyudadas){
        this.periodo = periodo;
        this.cantidadDonaciones = cantidadDonaciones;
        this.cantidadOrganizacionesAyudadas = cantidadOrganizacionesAyudadas;
    }
}
