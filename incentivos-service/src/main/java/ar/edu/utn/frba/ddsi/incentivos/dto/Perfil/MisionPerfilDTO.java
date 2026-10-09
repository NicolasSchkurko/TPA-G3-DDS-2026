package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import lombok.Getter;
import lombok.Setter;

/** Misión vigente de un donante, con su avance. */
@Getter
@Setter
public class MisionPerfilDTO {

    private String nombreMision;
    private String descripcion;
    private String insigniaObjetivo;

    /** Avance acumulado del donante en esta misión. */
    private Integer progresoActual;

    /** Donaciones necesarias para completar la misión. */
    private Integer progresoObjetivo;

    /** Donaciones que faltan. Es 0 cuando la misión ya está cumplida. */
    private Integer progresoFaltante;

    public MisionPerfilDTO(String nombreMision,
                           String descripcion,
                           String insigniaObjetivo,
                           Integer progresoActual,
                           Integer progresoObjetivo,
                           Integer progresoFaltante) {
        this.nombreMision = nombreMision;
        this.descripcion = descripcion;
        this.insigniaObjetivo = insigniaObjetivo;
        this.progresoActual = progresoActual;
        this.progresoObjetivo = progresoObjetivo;
        this.progresoFaltante = progresoFaltante;
    }
}
