package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;

public interface EstrategiaNotificacion {

    TipoEventoNotificacion getTipoEvento();

    void ejecutar(Object datos);

}
