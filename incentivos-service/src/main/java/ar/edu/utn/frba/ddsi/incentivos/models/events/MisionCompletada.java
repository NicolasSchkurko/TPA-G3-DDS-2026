package ar.edu.utn.frba.ddsi.incentivos.models.events;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import java.util.UUID;

/**
 * Se publica cuando un donante termina una misión y recibe su insignia.
 *
 * <p>Lo consumen {@code N8nClient}, que arma la publicación para las redes, y
 * {@code NotificacionClient}, que le avisa al donante.
 *
 * <p>Lleva el {@code idUsuario} en vez del medio de contacto resuelto a propósito: si el
 * evento llevara el contacto, habría que resolverlo <em>dentro</em> de la transacción que
 * otorga la insignia, y esa llamada HTTP no corresponde a la base. El listener lo busca en
 * su propio {@code AFTER_COMMIT}, cuando ya se sabe que el progreso quedó guardado.
 */
public record MisionCompletada(String misionAnterior,
                               String insigniaObtenida,
                               UUID idUsuario,
                               String nombreUsuario,
                               ImpactoDonacion impactoDonacion) {
}
