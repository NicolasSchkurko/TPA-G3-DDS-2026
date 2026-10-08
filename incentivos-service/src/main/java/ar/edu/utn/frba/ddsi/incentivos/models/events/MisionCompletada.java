package ar.edu.utn.frba.ddsi.incentivos.models.events;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import java.util.UUID;

/**
 * Se publica cuando un donante termina una misión y recibe su insignia. Lo consumen
 * {@code N8nClient} y {@code NotificacionClient}. Lleva {@code idUsuario} y no el contacto:
 * el listener lo resuelve en {@code AFTER_COMMIT}.
 */
public record MisionCompletada(String misionAnterior,
                               String insigniaObtenida,
                               UUID idUsuario,
                               String nombreUsuario,
                               ImpactoDonacion impactoDonacion) {
}
