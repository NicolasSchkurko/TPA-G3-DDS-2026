package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;

/** Estrategia de notificación (patrón Strategy): cada implementación encapsula la
 *  construcción del mensaje, los destinatarios y el envío por ServicioNotificaciones. */

public interface EstrategiaNotificacion {

    TipoEventoNotificacion getTipoEvento();

    void ejecutar(Object datos);

}
