package ar.edu.utn.frba.ddsi.donaciones.dto;

import java.time.LocalDate;
import java.util.List;

public class FormularioDTO {
  // Información del donante
  private String donanteName;

  // Metadata del formulario
  private LocalDate fechaRealizacion;

  // Donaciones registradas en el formulario (usando DonacionDTO completo)
  private List<DonacionDTO> donaciones;
}
