package ar.edu.utn.frba.ddsi.incentivos.models.events;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import java.util.UUID;

public record MisionCompletada(String misionAnterior,
							   String insigniaObtenida,
							   UUID idUsuario,
							   String nombreUsuario,
							   ImpactoDonacion impactoDonacion) {
}
