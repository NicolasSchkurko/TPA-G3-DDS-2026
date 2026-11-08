package ar.edu.utn.frba.ddsi.front.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EntidadBeneficiariaDTO {
    private String razonSocial;
    private String rubro;
    private String tipoJuridico;
    private String telefono;
    private Boolean activa;
}
