package ar.edu.utn.frba.ddsi.incentivos.controllers.request;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.UUID;

/**
 * Filtros del listado de categorías, con los mismos query params del endpoint.
 *
 * <p>Todos opcionales: en la query van con la forma {@code (:nombre IS NULL OR ...)}, así
 * que un parámetro ausente no cambia el resultado en vez de filtrar por null.
 */
public record CategoriaFiltroRequest(
    @Parameter(description = "Filtrar por nombre de categoría (búsqueda parcial)")
    String nombre,

    @Parameter(description = "Filtrar por posición exacta en la secuencia")
    Integer posicionSecuencia,

    @Parameter(description = "Filtrar categorías que contengan una misión específica (UUID)")
    UUID misionId
) {}