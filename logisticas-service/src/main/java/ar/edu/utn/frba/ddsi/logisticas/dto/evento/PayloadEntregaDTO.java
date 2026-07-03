package ar.edu.utn.frba.ddsi.logisticas.dto.evento;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PayloadEntregaDTO {
  private String fechaEntrega;
  private String horaEntrega;
  private String patenteCamion;
  private String nombreChofer;
}