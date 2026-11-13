package ar.edu.utn.frba.ddsi.donaciones.dto.donaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Donación sin la lista de bienes, para listados y resultados de matchmaking. */
@Getter
@Setter
public class DonacionResumenDTO {
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

  public static DonacionResumenDTO from(Donacion donacion) {
    if (donacion == null) return null;
    DonacionResumenDTO dto = new DonacionResumenDTO();
    dto.setId(donacion.getId());
    dto.setDonanteName((donacion.getDonante() != null && donacion.getDonante().getPersona() != null)
        ? donacion.getDonante().getPersona().getNombreDeUsuario() : "Desconocido");
    dto.setEntidadBeneficiaria((donacion.getEntidad() != null
        && donacion.getEntidad().getPersonaJuridica() != null)
        ? donacion.getEntidad().getPersonaJuridica().getRazonSocial() : "No asignada");
    dto.setDescripcion(donacion.getDescripcion());
    dto.setEstado(donacion.getEstado() != null ? donacion.getEstado().name() : "N/A");
    dto.setSubcategoriaName(donacion.getSubcategoria() != null
        ? donacion.getSubcategoria().getNombre() : "N/A");
    dto.setCategoriaBienName((donacion.getSubcategoria() != null
        && donacion.getSubcategoria().getCategoria() != null)
        ? donacion.getSubcategoria().getCategoria().getNombre() : "N/A");
    dto.setFechaEntrega(donacion.getFechaEntrega());
    dto.setCantidadTotalBienes(donacion.sumaCantidadBienes());
    dto.setImagenes(donacion.urlsDeImagenes());
    return dto;
  }
}
