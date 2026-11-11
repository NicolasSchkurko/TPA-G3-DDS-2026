package ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Datos mínimos de una entidad para embeber en propuestas de matchmaking. */
@Getter
@Setter
public class EntidadBeneficiariaResumenDTO {
  private UUID id;
  private String razonSocial;
  private String rubro;
  private Boolean activa;

  public static EntidadBeneficiariaResumenDTO from(EntidadBeneficiaria entidad) {
    if (entidad == null) return null;
    EntidadBeneficiariaResumenDTO dto = new EntidadBeneficiariaResumenDTO();
    dto.setId(entidad.getId());
    dto.setActiva(entidad.isActiva());
    if (entidad.getPersonaJuridica() != null) {
      dto.setRazonSocial(entidad.getPersonaJuridica().getRazonSocial());
      dto.setRubro(entidad.getPersonaJuridica().getRubro());
    }
    return dto;
  }
}
