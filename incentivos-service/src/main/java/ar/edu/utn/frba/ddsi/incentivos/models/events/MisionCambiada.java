package ar.edu.utn.frba.ddsi.incentivos.models.events;

import java.util.UUID;

public record MisionCambiada(String misionAnterior,
                             String insigniaAnterior,
                             String nombreUsuario,
                             UUID idUsuario,
                             String misionNueva) {
}
