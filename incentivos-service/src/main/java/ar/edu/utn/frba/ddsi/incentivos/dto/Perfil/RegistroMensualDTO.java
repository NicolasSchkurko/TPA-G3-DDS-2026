package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import lombok.Getter;
import lombok.Setter;

import java.time.YearMonth;

@Getter
@Setter
public class RegistroMensualDTO {
    private YearMonth periodo;

    // Cambiamos a Long anticipando las queries dinámicas de SQL (COUNT)
    private Long cantidadDonaciones;
    private Long cantidadOrganizacionesAyudadas;

    public RegistroMensualDTO(YearMonth periodo, Long cantidadDonaciones, Long cantidadOrganizacionesAyudadas){
        this.periodo = periodo;
        this.cantidadDonaciones = cantidadDonaciones;
        this.cantidadOrganizacionesAyudadas = cantidadOrganizacionesAyudadas;
    }
}