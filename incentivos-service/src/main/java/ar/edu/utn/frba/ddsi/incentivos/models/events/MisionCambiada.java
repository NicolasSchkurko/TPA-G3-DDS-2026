package ar.edu.utn.frba.ddsi.incentivos.models.events;

import java.util.UUID;

/**
 * El donante pasó a una misión nueva. No lleva el contacto resuelto: el listener lo busca en
 * {@code AFTER_COMMIT}, fuera de la transacción.
 */
public record MisionCambiada(String misionAnterior,
                             String insigniaAnterior,
                             String nombreUsuario,
                             UUID idUsuario,
                             String misionNueva) {
}
