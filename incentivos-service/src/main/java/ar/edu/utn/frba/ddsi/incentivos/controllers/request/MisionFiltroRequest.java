package ar.edu.utn.frba.ddsi.incentivos.controllers.request;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * Filtros del listado de misiones, con los mismos query params del endpoint.
 *
 * <p>Los tres son opcionales y se combinan con AND. La búsqueda por nombre es parcial y
 * normalizada a minúsculas en los dos lados, así que no distingue mayúsculas.
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