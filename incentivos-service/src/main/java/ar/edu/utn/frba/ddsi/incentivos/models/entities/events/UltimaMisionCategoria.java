package ar.edu.utn.frba.ddsi.incentivos.models.entities.events;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;

import java.util.UUID;

public record UltimaMisionCategoria(UUID idCategoriaCompletada, Perfil perfil) {
}
