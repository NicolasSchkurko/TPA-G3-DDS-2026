package ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.BienResumenDTO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FormularioRequestDTO {
  private UUID idDonante;
  private List<BienResumenDTO> bienes;
  private LocalDate fechaRealizacion;
}