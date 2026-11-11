package ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Datos mínimos de una necesidad para embeber en propuestas de matchmaking. */
@Getter
@Setter
public class NecesidadResumenDTO {
  private UUID id;
  private String descripcion;
  private Integer cantidadObjetivo;
  private String nombreSubcategoria;
  private Boolean activa;

  public static NecesidadResumenDTO from(Necesidad necesidad) {
    if (necesidad == null) return null;
    NecesidadResumenDTO dto = new NecesidadResumenDTO();
    dto.setId(necesidad.getId());
    dto.setDescripcion(necesidad.getDescripcion());
    dto.setCantidadObjetivo(necesidad.getCantidadObjetivo());
    dto.setActiva(necesidad.isActiva());
    if (necesidad.getSubcategoria() != null) {
      dto.setNombreSubcategoria(necesidad.getSubcategoria().getNombre());
    }
    return dto;
  }
}
