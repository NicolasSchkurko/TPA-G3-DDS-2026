package ar.edu.utn.frba.ddsi.incentivos.models.entities.ServicioMensaje.EstrategiaMensaje.java;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.ServicioMensaje.EstrategiaNotificacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.ServicioNotificaciones.ServicioNotificaciones;

/**
 * Una estrategia de mensaje base en caso de que se tenga que repetir logica
 */

public abstract class EstrategiaMensaje implements EstrategiaNotificacion {

    protected final ServicioNotificaciones servicioNotificaciones;

    protected EstrategiaMensaje(
            ServicioNotificaciones servicioNotificaciones) {

        this.servicioNotificaciones = servicioNotificaciones;
    }

    protected String valorOTexto(
            String valor,
            String textoPorDefecto) {

        if (valor == null || valor.isBlank()) {
            return textoPorDefecto;
        }

        return valor;
    }

}
