package ar.edu.utn.frba.ddsi.donaciones.dto.logistica;

import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.DireccionDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DireccionEntidadDTO {
    private UUID idEntidad;
    private DireccionDTO direccion;
}
