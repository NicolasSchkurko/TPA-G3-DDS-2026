package ar.edu.utn.frba.ddsi.incentivos.controllers.request;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.UUID;

public record CategoriaFiltroRequest(
    @Parameter(description = "Filtrar por nombre de categoría (búsqueda parcial)")
    String nombre,

    @Parameter(description = "Filtrar por posición exacta en la secuencia")
    Integer posicionSecuencia,

    @Parameter(description = "Filtrar categorías que contengan una misión específica (UUID)")
    UUID misionId
) {}