package ar.edu.utn.frba.ddsi.incentivos.models.events;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;

import java.util.UUID;

public record MisionCambiada(String misionAnterior,
                             String insigniaAnterior,
                             String nombreUsuario,
                             UUID idUsuario,
                             MedioContacto contacto,
                             String misionNueva) {
}
