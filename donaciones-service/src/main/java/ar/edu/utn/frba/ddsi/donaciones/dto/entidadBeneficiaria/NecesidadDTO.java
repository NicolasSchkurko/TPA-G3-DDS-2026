package ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadExtraordinaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadRecurrente;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class NecesidadDTO {
    private String tipoNecesidad;
    private String descripcion;
    private Integer cantidadObjetivo;
    private String nombreSubcategoria;
    private String nombreCategoria;
    private Integer plazoEnDias;
    private UUID id;
    public Necesidad toDomain() {
        CategoriaBien cat = new CategoriaBien(this.nombreCategoria != null ? this.nombreCategoria : "General");
        SubcategoriaBien sub = new SubcategoriaBien(this.nombreSubcategoria != null ? this.nombreSubcategoria : "General", cat);
        String tipo = this.tipoNecesidad != null ? this.tipoNecesidad.toUpperCase() : "RECURRENTE";

        if ("EXTRAORDINARIA".equals(tipo)) {
            return new NecesidadExtraordinaria(sub, this.descripcion, this.cantidadObjetivo);
        }
        return new NecesidadRecurrente(sub, this.descripcion, this.cantidadObjetivo, this.plazoEnDias != null ? this.plazoEnDias : 30);
    }

    public static NecesidadDTO from(Necesidad necesidad) {
        if (necesidad == null) return null;
        NecesidadDTO dto = new NecesidadDTO();
        dto.setDescripcion(necesidad.getDescripcion());
        dto.setCantidadObjetivo(necesidad.getCantidadObjetivo());
        if (necesidad.getSubcategoria() != null) {
            dto.setNombreSubcategoria(necesidad.getSubcategoria().getNombre());
            if (necesidad.getSubcategoria().getCategoria() != null) dto.setNombreCategoria(necesidad.getSubcategoria().getCategoria().getNombre());
        }
        dto.setId(necesidad.getId());
        dto.setTipoNecesidad(necesidad instanceof NecesidadRecurrente ? "RECURRENTE" : "EXTRAORDINARIA");
        if (necesidad instanceof NecesidadRecurrente recurrente) dto.setPlazoEnDias(recurrente.getPlazoEnDias());
        return dto;
    }
}