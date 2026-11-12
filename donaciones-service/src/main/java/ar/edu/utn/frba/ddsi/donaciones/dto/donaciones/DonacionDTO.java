package ar.edu.utn.frba.ddsi.donaciones.dto.donaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DonacionDTO {
  private UUID id;
  private String donanteName;
  private String entidadBeneficiaria;
  private String descripcion;
  private String estado;
  private String subcategoriaName;
  private String categoriaBienName;
  private LocalDate fechaEntrega;
  private Integer cantidadTotalBienes;
  private List<String> imagenes = new ArrayList<>();
  // Cascada de validación (punto 22): cada BienResumenDTO de la lista se valida también.
  @Valid
  private List<BienResumenDTO> bienes;

  public Donacion toDomain() {
    Donacion donacion = new Donacion();
    if (this.id != null) donacion.setId(this.id);
    donacion.setDescripcion(this.descripcion);
    if (this.bienes != null) {
      donacion.setBienes(this.bienes.stream().map(BienResumenDTO::toDomain).collect(Collectors.toList()));
    }
    return donacion;
  }

  public static DonacionDTO from(Donacion donacion) {
    if (donacion == null) return null;
    DonacionResumenDTO resumen = DonacionResumenDTO.from(donacion);
    DonacionDTO dto = new DonacionDTO();
    dto.setId(resumen.getId());
    dto.setDonanteName(resumen.getDonanteName());
    dto.setEntidadBeneficiaria(resumen.getEntidadBeneficiaria());
    dto.setDescripcion(resumen.getDescripcion());
    dto.setEstado(resumen.getEstado());
    dto.setSubcategoriaName(resumen.getSubcategoriaName());
    dto.setCategoriaBienName(resumen.getCategoriaBienName());
    dto.setFechaEntrega(resumen.getFechaEntrega());
    dto.setCantidadTotalBienes(resumen.getCantidadTotalBienes());
    dto.setImagenes(resumen.getImagenes() != null ? resumen.getImagenes() : new ArrayList<>());

    if (donacion.getBienes() != null) {
      dto.setBienes(donacion.getBienes().stream().map(BienResumenDTO::from).collect(Collectors.toList()));
    } else {
      dto.setBienes(new ArrayList<>());
    }

    return dto;
  }
}