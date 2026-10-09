package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import java.time.YearMonth;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistroMensualDTO {
    private YearMonth periodo;

    // Long porque el COUNT de SQL devuelve Long.
    private Long cantidadDonaciones;
    private Long cantidadOrganizacionesAyudadas;

    public RegistroMensualDTO(YearMonth periodo, Long cantidadDonaciones, Long cantidadOrganizacionesAyudadas) {
        this.periodo = periodo;
        this.cantidadDonaciones = cantidadDonaciones;
        this.cantidadOrganizacionesAyudadas = cantidadOrganizacionesAyudadas;
    }
}
