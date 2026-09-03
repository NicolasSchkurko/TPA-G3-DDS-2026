package ar.edu.utn.frba.ddsi.incentivos.models.events;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import java.util.UUID;

public record CategoriaNuevaPublicar(String categoriaAnterior,
                                     String categoriaNueva,
                                     String nombreUsuario,
                                     MedioContacto contacto) {
}
