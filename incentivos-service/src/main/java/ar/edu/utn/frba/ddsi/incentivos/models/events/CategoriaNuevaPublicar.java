package ar.edu.utn.frba.ddsi.incentivos.models.events;

import java.util.UUID;

public record CategoriaNuevaPublicar(String categoriaAnterior,
                                     String categoriaNueva,
                                     String nombreUsuario,
                                     UUID idUsuario) {
}
