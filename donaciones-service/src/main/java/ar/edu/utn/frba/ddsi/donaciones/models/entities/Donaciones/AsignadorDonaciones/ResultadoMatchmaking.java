package ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.AsignadorDonaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResultadoMatchmaking {

  private final Donacion donacion;

  // Lista unificada de propuestas con sus respectivos metadatos de algoritmos
  private final List<PropuestaAsignacion> propuestasOrdenadas;

  // Flag para la Interfaz de Usuario
  private final boolean huboCoincidenciaTotal;
}