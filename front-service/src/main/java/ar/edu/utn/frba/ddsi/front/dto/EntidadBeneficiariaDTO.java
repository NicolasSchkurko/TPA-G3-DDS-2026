package ar.edu.utn.frba.ddsi.front.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** DTO de lectura para las entidades devueltas por donaciones-service. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntidadBeneficiariaDTO {
    private String razonSocial;
    private String rubro;
    private String cuit;
    private String tipoJuridico;
    private String telefono;
    private String correoElectronico;
    private DireccionBeneficiariaDTO direccion;
    private UUID id;
    private Boolean activa;
}
