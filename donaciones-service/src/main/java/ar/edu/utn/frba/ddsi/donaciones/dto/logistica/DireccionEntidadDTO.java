package ar.edu.utn.frba.ddsi.donaciones.dto.logistica;

import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.DireccionDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DireccionEntidadDTO {
    private String nombreEntidad;
    private DireccionDTO direccion;
}
