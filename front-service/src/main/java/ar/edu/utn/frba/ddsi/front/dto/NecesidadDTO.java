package ar.edu.utn.frba.ddsi.front.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** DTO de necesidad alineado con NecesidadDTO de donaciones-service. */
@Getter
@Setter
@NoArgsConstructor
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
    private List<DonacionDTO> donaciones = new ArrayList<>();

    /** Bienes recibidos/asignados que cuentan para el progreso de la necesidad. */
    public int bienesRecibidos() {
        if (donaciones == null) return 0;
        return donaciones.stream()
                .filter(d -> d != null && d.getCantidadTotalBienes() != null)
                .filter(d -> estadoCuenta(d.getEstado()))
                .mapToInt(DonacionDTO::getCantidadTotalBienes)
                .sum();
    }

    public int progreso() {
        if (cantidadObjetivo == null || cantidadObjetivo <= 0) return 0;
        return (int) Math.min(100, Math.round(bienesRecibidos() * 100.0 / cantidadObjetivo));
    }

    private static boolean estadoCuenta(String estado) {
        if (estado == null) return false;
        return switch (estado.toUpperCase()) {
            case "ASIGNADO", "EN_TRASLADO", "ENTREGADO" -> true;
            default -> false;
        };
    }
}
