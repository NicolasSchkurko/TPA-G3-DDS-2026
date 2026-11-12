package ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria;

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
    private String unidadDeMedida;
    private String nombreSubcategoria;
    private String nombreCategoria;
    private Integer plazoEnDias;
    private UUID id;
    private Boolean activa;

    /**
     * Construye la Necesidad de dominio a partir del DTO.
     * La SubcategoriaBien se recibe ya resuelta (buscada o creada previamente,
     * ver RepositorioDeSubcategoriasDeBienes.obtenerOCrearSubcategoria) para no duplicar entradas
     * del catálogo de Bienes en cada alta.
     */
    public Necesidad toDomain(SubcategoriaBien subcategoria) {
        // cantidadObjetivo/plazoEnDias nulos o no positivos no pueden llegar a persistirse:
        // Necesidad.cantidadObjetivo es un Integer (autoboxing), así que un null explota con
        // NPE recién en estaSatisfecha() -- en el scheduler de matchmaking, no en el alta. Y un
        // plazoEnDias <= 0 rompe NecesidadRecurrente.cantidadRecibidaEnPeriodo (0 días nunca
        // cuenta nada, negativo cuenta todo el histórico).
        if (this.cantidadObjetivo == null || this.cantidadObjetivo <= 0) {
            throw new IllegalArgumentException("cantidadObjetivo es obligatorio y debe ser positivo");
        }
        if (this.plazoEnDias != null && this.plazoEnDias <= 0) {
            throw new IllegalArgumentException("plazoEnDias debe ser positivo");
        }

        String tipo = this.tipoNecesidad != null ? this.tipoNecesidad.toUpperCase() : "RECURRENTE";

        Necesidad necesidad;
        if ("EXTRAORDINARIA".equals(tipo)) {
            necesidad = new NecesidadExtraordinaria(subcategoria, this.descripcion, this.cantidadObjetivo);
        } else {
            necesidad = new NecesidadRecurrente(subcategoria, this.descripcion, this.cantidadObjetivo, this.plazoEnDias != null ? this.plazoEnDias : 30);
        }

        if (this.id != null) {
            necesidad.setId(this.id);
        }

        if (this.activa != null) {
            necesidad.setActiva(this.activa);
        }
        necesidad.setUnidadDeMedida(normalizarUnidad(this.unidadDeMedida));

        return necesidad;
    }

    public static NecesidadDTO from(Necesidad necesidad) {
        if (necesidad == null) return null;
        NecesidadDTO dto = new NecesidadDTO();
        dto.setDescripcion(necesidad.getDescripcion());
        dto.setCantidadObjetivo(necesidad.getCantidadObjetivo());
        dto.setUnidadDeMedida(necesidad.getUnidadDeMedida());
        if (necesidad.getSubcategoria() != null) {
            dto.setNombreSubcategoria(necesidad.getSubcategoria().getNombre());
            if (necesidad.getSubcategoria().getCategoria() != null) dto.setNombreCategoria(necesidad.getSubcategoria().getCategoria().getNombre());
        }
        dto.setId(necesidad.getId());
        dto.setActiva(necesidad.isActiva());
        dto.setTipoNecesidad(necesidad instanceof NecesidadRecurrente ? "RECURRENTE" : "EXTRAORDINARIA");
        if (necesidad instanceof NecesidadRecurrente recurrente) dto.setPlazoEnDias(recurrente.getPlazoEnDias());

        return dto;
    }

    private static String normalizarUnidad(String unidad) {
        if (unidad == null || unidad.isBlank()) {
            return "UNIDADES";
        }
        return switch (unidad.trim().toUpperCase()) {
            case "KILOGRAMOS", "KILOS", "KG" -> "KILOGRAMOS";
            case "LITROS", "LT" -> "LITROS";
            default -> "UNIDADES";
        };
    }
}
