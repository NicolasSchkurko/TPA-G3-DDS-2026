package ar.edu.utn.frba.ddsi.notificaciones.models.entities.CanalNotificacion;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Destinatario.Destinatario;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;

public abstract class CanalNotificacion {
    public abstract boolean enviar(Notificacion notificacion, Destinatario destinatario);
}
