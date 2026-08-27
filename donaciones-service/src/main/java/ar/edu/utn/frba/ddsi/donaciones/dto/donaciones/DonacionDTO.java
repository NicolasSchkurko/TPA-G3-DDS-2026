package ar.edu.utn.frba.ddsi.donaciones.dto.donaciones;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DonacionDTO {

  // Información del donante
  private String donanteName;

  // Información de la entidad beneficiaria (si ya está asignada)
  private String entidadBeneficiaria;

  // Metadata de la donación
  private String descripcion;
  private String estado;
  private String subcategoriaName;
  private String categoriaBienName;
  private LocalDate fechaEntrega;

  // Información de los bienes
  private Integer cantidadTotalBienes;
  private List<BienResumenDTO> bienes;
}