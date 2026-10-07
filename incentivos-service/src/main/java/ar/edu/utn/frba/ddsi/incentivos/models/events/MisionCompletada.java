package ar.edu.utn.frba.ddsi.incentivos.models.events;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import java.util.UUID;

/**
 * Se publica cuando un donante termina una misión y recibe su insignia.
 *
 * <p>Lo consumen {@code N8nClient}, que arma la publicación para las redes, y
 * {@code NotificacionClient}, que le avisa al donante.
 *
 * <p>Lleva el {@code idUsuario} en vez del medio de contacto resuelto a propósito. El
 * listener de notificaciones resuelve el contacto después del commit, mientras que el de
 * n8n guarda el contenido del webhook en la outbox dentro de la transacción; ninguno hace
 * una llamada HTTP dentro de la transacción que otorga la insignia.
 */
public record MisionCompletada(String misionAnterior,
                               String insigniaObtenida,
                               UUID idUsuario,
                               String nombreUsuario,
                               ImpactoDonacion impactoDonacion) {
}
