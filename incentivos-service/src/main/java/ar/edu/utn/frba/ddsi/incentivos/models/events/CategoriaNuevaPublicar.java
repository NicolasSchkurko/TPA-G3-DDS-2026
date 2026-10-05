package ar.edu.utn.frba.ddsi.incentivos.models.events;

import java.util.UUID;

/**
 * El donante pasó a una categoría nueva.
 *
 * <p><b>No lleva el contacto resuelto</b> por la misma razón que {@link MisionCambiada}: se
 * resuelve en el listener, que corre en {@code AFTER_COMMIT} y por lo tanto fuera de la
 * transacción (punto 12). Antes se pedía dentro y la transacción retenía una conexión del
 * pool durante la llamada HTTP.
 *
 * <p>Por eso este record lleva {@code idUsuario}: el listener necesita poder consultar el
 * contacto de alguien.
 */
public record CategoriaNuevaPublicar(String categoriaAnterior,
                                     String categoriaNueva,
                                     String nombreUsuario,
                                     UUID idUsuario) {
}