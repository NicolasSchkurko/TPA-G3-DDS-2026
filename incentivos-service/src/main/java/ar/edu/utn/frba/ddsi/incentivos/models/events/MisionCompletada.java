package ar.edu.utn.frba.ddsi.incentivos.models.events;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;

public record MisionCompletada (ImpactoDonacion impactoDonacion, Perfil perfilActualizado){
}
