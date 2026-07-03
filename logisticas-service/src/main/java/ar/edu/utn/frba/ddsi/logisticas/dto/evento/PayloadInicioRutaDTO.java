package ar.edu.utn.frba.ddsi.logisticas.dto.evento;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PayloadInicioRutaDTO {
  private List<String> items;
  private String urlRuta;
}