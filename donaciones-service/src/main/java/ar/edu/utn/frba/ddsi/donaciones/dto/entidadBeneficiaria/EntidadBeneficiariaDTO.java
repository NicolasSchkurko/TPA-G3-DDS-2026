package ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntidadBeneficiariaDTO {
    private String razonSocial;
    private String telefono;
    private DireccionDTO direccion;
}
