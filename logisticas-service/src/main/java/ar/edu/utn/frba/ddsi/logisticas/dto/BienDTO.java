package ar.edu.utn.frba.ddsi.logisticas.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BienDTO {
    private UUID idBien;
    private String subCategoria;
    private String categoria;
    private String descripcion;
    private String urlImg;
    private Integer cantidad;
    private String unidadMedida;
}
