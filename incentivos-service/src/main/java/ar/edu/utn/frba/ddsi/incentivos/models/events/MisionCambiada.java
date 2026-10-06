package ar.edu.utn.frba.ddsi.incentivos.models.events;

import java.util.UUID;

/**
 * El donante pasó a una misión nueva.
 *
 * <p><b>No lleva el contacto resuelto, y es a propósito</b> (punto 12). Antes el
 * `MedioContacto` se pedía a `donaciones-service` *dentro* de la transacción, se guardaba
 * en el evento y solo se usaba acá, en el listener. O sea que la transacción retenía una
 * conexión del pool durante una llamada HTTP, y eso es justo lo que hace que un downstream
 * colgado tumbe el servicio.
 *
 * <p>Este evento se escucha en {@code AFTER_COMMIT}, o sea que ya estamos fuera de la
 * transacción: el contacto se resuelve acá, donde no cuesta una conexión.
 *
 * <p>Es el mismo criterio que ya usa {@link MisionCompletada}.
 */
public record MisionCambiada(String misionAnterior,
                             String insigniaAnterior,
                             String nombreUsuario,
                             UUID idUsuario,
                             String misionNueva) {
}
