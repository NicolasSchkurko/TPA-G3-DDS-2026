package ar.edu.utn.frba.ddsi.incentivos.models.entities.ServicioMensaje;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.ServicioNotificaciones.TipoEventoNotificacion;

/**
 * Uso de patron Strategy para el llamado al servicio de notificaciones
 * ---------
 * Define la lógica de notificación para un tipo de evento específico.
 * Cada estrategia encapsula la construcción del mensaje, la selección
 * de destinatarios y el envío mediante el ServicioNotificaciones.
 */

public interface EstrategiaNotificacion {

    TipoEventoNotificacion getTipoEvento();

    void ejecutar(Object datos);

}
