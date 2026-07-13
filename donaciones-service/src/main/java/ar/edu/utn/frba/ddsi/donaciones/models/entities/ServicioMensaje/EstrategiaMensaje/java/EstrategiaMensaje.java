package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiaMensaje.java;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiaNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.ServicioNotificaciones;

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
