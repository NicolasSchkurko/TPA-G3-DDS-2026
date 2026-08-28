package ar.edu.utn.frba.ddsi.donaciones.dto.donaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DonacionDTO {
  private String donanteName;
  private String entidadBeneficiaria;
  private String descripcion;
  private String estado;
  private String subcategoriaName;
  private String categoriaBienName;
  private LocalDate fechaEntrega;
  private Integer cantidadTotalBienes;
  private List<BienResumenDTO> bienes;

  public Donacion toDomain() {
    Donacion donacion = new Donacion();
    donacion.setDescripcion(this.descripcion);
    if (this.bienes != null) {
      donacion.setBienes(this.bienes.stream().map(BienResumenDTO::toDomain).collect(Collectors.toList()));
    }
    return donacion;
  }

  public static DonacionDTO from(Donacion donacion) {
    if (donacion == null) return null;
    DonacionDTO dto = new DonacionDTO();
    dto.setDonanteName((donacion.getDonante() != null && donacion.getDonante().getPersona() != null) ? donacion.getDonante().getPersona().getNombreDeUsuario() : "Desconocido");
    dto.setEntidadBeneficiaria((donacion.getEntidad() != null && donacion.getEntidad().getPersonaJuridica() != null) ? donacion.getEntidad().getPersonaJuridica().getRazonSocial() : "No asignada");
    dto.setDescripcion(donacion.getDescripcion());
    dto.setEstado(donacion.getEstado() != null ? donacion.getEstado().name() : "N/A");
    dto.setSubcategoriaName(donacion.getSubcategoria() != null ? donacion.getSubcategoria().getNombre() : "N/A");
    dto.setCategoriaBienName((donacion.getSubcategoria() != null && donacion.getSubcategoria().getCategoria() != null) ? donacion.getSubcategoria().getCategoria().getNombre() : "N/A");
    dto.setFechaEntrega(donacion.getFechaEntrega());
    dto.setCantidadTotalBienes(donacion.sumaCantidadBienes());
    dto.setBienes(new ArrayList<>());
    return dto;
  }
}