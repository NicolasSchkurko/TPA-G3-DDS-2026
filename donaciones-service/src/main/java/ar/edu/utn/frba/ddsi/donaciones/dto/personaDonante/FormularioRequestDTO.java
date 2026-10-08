package ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.BienResumenDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FormularioRequestDTO {
  @NotNull(message = "idDonante es obligatorio")
  private UUID idDonante;

  @Valid
  private List<BienResumenDTO> bienes;

  private LocalDate fechaRealizacion;
}