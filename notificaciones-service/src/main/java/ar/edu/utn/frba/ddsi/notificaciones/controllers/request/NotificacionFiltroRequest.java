package ar.edu.utn.frba.ddsi.notificaciones.controllers.request;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import io.swagger.v3.oas.annotations.Parameter;

/** Filtros del listado; el estado y el destinatario ausentes no filtran. */
public record NotificacionFiltroRequest(
        @Parameter(description = "Filtrar por estado de la notificación")
        EstadoNotificacion estado,
        @Parameter(description = "Filtrar por dirección de contacto (email o teléfono)")
        String destinatario) {
}
