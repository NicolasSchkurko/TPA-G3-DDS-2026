package ar.edu.utn.frba.ddsi.incentivos.models.events;

import java.util.UUID;

/**
 * El donante pasó a una categoría nueva. No lleva el contacto: el listener lo resuelve en
 * {@code AFTER_COMMIT} a partir del {@code idUsuario}.
 */
public record CategoriaNuevaPublicar(String categoriaAnterior,
                                     String categoriaNueva,
                                     String nombreUsuario,
                                     UUID idUsuario) {
}
