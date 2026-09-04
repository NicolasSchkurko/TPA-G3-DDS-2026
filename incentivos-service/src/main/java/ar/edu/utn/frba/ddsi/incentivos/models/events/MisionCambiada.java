package ar.edu.utn.frba.ddsi.incentivos.models.events;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;

public record MisionCambiada(String misionAnterior,
                             String insigniaAnterior,
                             String nombreUsuario,
                             MedioContacto contacto,
                             String misionNueva) {
}
