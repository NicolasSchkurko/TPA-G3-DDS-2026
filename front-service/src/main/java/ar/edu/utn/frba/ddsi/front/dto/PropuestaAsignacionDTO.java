package ar.edu.utn.frba.ddsi.front.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una entidad candidata para recibir una donación, tal como la propone el asignador.
 * Espejo del PropuestaAsignacionDTO de donaciones-service.
 */
@Getter
@Setter
@NoArgsConstructor
public class PropuestaAsignacionDTO {

    private EntidadBeneficiariaDTO entidad;
    private NecesidadDTO necesidad;

    /** "CompatibilidadSemantica", "SubAtendidos" o "Interseccion" si coincidieron los dos. */
    private String algoritmo;

    /** 1-based: es la posición que viaja de vuelta en POST /api/donaciones/asignar. */
    private int posicion;

    private double score;

    /** La mejor propuesta del ranking, la que el asignador pondría primero. */
    public boolean esRecomendada() {
        return posicion == 1;
    }

    public String etiquetaAlgoritmo() {
        if (algoritmo == null || algoritmo.isBlank()) {
            return "Sin algoritmo";
        }
        return switch (algoritmo) {
            case "CompatibilidadSemantica" -> "Compatibilidad semántica";
            case "SubAtendidos" -> "Entidades sub-atendidas";
            case "Interseccion" -> "Coinciden los dos algoritmos";
            default -> algoritmo;
        };
    }

    /**
     * El score no significa lo mismo según de dónde salga la propuesta, así que se muestra como
     * dato de auditoría y no como "puntaje" a secas: en los algoritmos individuales es una
     * proporción entre 0 y 1 (cuánto cubre la donación de lo que falta), mientras que en la
     * intersección es la suma de las posiciones en cada ranking, donde más bajo es mejor.
     */
    public String scoreLegible() {
        if ("Interseccion".equals(algoritmo)) {
            return "suma de posiciones: " + Math.round(score);
        }
        return "afinidad: " + Math.round(score * 100) + "%";
    }
}
