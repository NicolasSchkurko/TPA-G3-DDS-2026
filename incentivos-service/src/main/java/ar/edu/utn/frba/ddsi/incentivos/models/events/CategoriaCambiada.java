package ar.edu.utn.frba.ddsi.incentivos.models.events;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;

import java.util.UUID;

public record CategoriaCambiada(Categoria categoriaAnterior,
                                Categoria categoriaNueva,
                                UUID idPerfil) {
}
