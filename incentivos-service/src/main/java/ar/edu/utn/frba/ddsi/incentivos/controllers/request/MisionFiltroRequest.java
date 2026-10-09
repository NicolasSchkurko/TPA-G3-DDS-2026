package ar.edu.utn.frba.ddsi.incentivos.controllers.request;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * Filtros del listado de misiones. Los tres son opcionales y se combinan con AND; la búsqueda
 * por nombre es parcial y sin distinguir mayúsculas.
 */
public record MisionFiltroRequest(
    @Parameter(description = "Filtrar por nombre de la misión (búsqueda parcial)")
    String nombreMision,

    @Parameter(description = "Filtrar por nombre de la insignia objetivo (búsqueda parcial)")
    String insigniaObjetivo,

    @Parameter(description = "Filtrar por atributo de impacto de la regla (ej. CANTIDAD_DONACIONES, PUNTOS, etc.)")
    String atributo
) {
}