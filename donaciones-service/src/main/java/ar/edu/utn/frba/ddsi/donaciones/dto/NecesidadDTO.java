package ar.edu.utn.frba.ddsi.donaciones.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NecesidadDTO {
    // Discriminador: "RECURRENTE" o "EXTRAORDINARIA"
    private String tipoNecesidad;

    // Campos comunes
    private String descripcion;
    private Integer cantidadObjetivo;
    private String nombreSubcategoria;
    private String nombreCategoria;
    // Solo para RECURRENTE
    private Integer plazoEnDias;
}
