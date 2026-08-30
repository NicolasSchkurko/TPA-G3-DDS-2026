package ar.edu.utn.frba.ddsi.donaciones.dto.donaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class BienResumenDTO {
  private String descripcion;
  private String subcategoria;
  private String categoria;
  private Integer cantidad;
  private String unidadDeMedida;
  private String urlFoto;
  private String tipoBien;
  private Boolean usado;
  private LocalDate fechaVencimiento;

  public Bien toDomain() {
    String nombreCat = (categoria != null && !categoria.trim().isEmpty()) ? categoria : "General";
    String nombreSub = (subcategoria != null && !subcategoria.trim().isEmpty()) ? subcategoria : "General";
    CategoriaBien cat = new CategoriaBien(nombreCat);
    SubcategoriaBien subcat = new SubcategoriaBien(nombreSub, cat);

    UnidadDeMedida um = null;
    if (unidadDeMedida != null) {
      um = switch (unidadDeMedida.toUpperCase()) {
        case "KILOGRAMOS", "KILOS", "KG" -> UnidadDeMedida.KILOGRAMOS;
        case "LITROS", "LT" -> UnidadDeMedida.LITROS;
        default -> null;
      };
    }

    if (tipoBien == null) return null;
    return switch (tipoBien.toUpperCase()) {
      case "CON_ESTADO", "CONESTADO" -> new BienConEstado(descripcion, subcat, urlFoto, cantidad != null ? cantidad : 0, um, usado != null ? usado : false);
      case "PERECEDERO" -> new BienPerecedero(descripcion, subcat, urlFoto, cantidad != null ? cantidad : 0, um, fechaVencimiento != null ? fechaVencimiento : LocalDate.now().plusMonths(1));
      default -> throw new IllegalArgumentException("Tipo de bien desconocido: " + tipoBien);
    };
  }

  public static BienResumenDTO from(Bien bien) {
    if (bien == null) return null;
    BienResumenDTO dto = new BienResumenDTO();
    dto.setDescripcion(bien.getDescripcion());
    if (bien.getSubcategoria() != null) {
      dto.setSubcategoria(bien.getSubcategoria().getNombre());
      if (bien.getSubcategoria().getCategoria() != null) dto.setCategoria(bien.getSubcategoria().getCategoria().getNombre());
    }
    dto.setCantidad(bien.getPeso());
    if (bien.getUnidadUtilizada() != null) dto.setUnidadDeMedida(bien.getUnidadUtilizada().name());

    if (bien instanceof BienConEstado bce) {
      dto.setTipoBien("CON_ESTADO");
      dto.setUrlFoto(bce.getUrlFoto());
      dto.setUsado(bce.isUsado());
    } else if (bien instanceof BienPerecedero bp) {
      dto.setTipoBien("PERECEDERO");
      dto.setUrlFoto(bp.getUrlFoto());
      dto.setFechaVencimiento(bp.getFechaVencimiento());
    }
    return dto;
  }
}