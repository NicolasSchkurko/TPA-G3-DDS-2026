package ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

// Resumen de lectura para Formulario: antes no existía ningún endpoint para listar/borrar
// formularios (RepositorioFormularios.obtenerTodos()/eliminarPorId() estaban sin usar desde
// ningún controller), lo que hacía estructuralmente imposible borrar un Donante que ya hubiera
// donado (Formulario.donante_id es FK no nula, sin cascade REMOVE a propósito -- ver comentario
// en Formulario.java). Este DTO solo expone lo necesario para identificar y borrar formularios
// desde fuera, no se usa para crear (eso lo sigue haciendo FormularioRequestDTO).
@Getter
@Setter
public class FormularioResumenDTO {
  private UUID id;
  private UUID donanteId;
  private LocalDate fechaRealizacion;

  public static FormularioResumenDTO from(Formulario formulario) {
    if (formulario == null) return null;
    FormularioResumenDTO dto = new FormularioResumenDTO();
    dto.setId(formulario.getId());
    dto.setDonanteId(formulario.getDonante() != null ? formulario.getDonante().getId() : null);
    dto.setFechaRealizacion(formulario.getFechaRealizacion());
    return dto;
  }
}
