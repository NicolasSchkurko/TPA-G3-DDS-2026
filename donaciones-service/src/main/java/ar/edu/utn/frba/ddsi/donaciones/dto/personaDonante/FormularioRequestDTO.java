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
  @NotNull(message = "El formulario requiere el id del donante")
  private UUID idDonante;

  // @Valid hace que cada BienResumenDTO de la lista también se valide (punto 22).
  @Valid
  private List<BienResumenDTO> bienes;

  // Obligatoria: es la fechaEntrega que va a incentivos (punto 30) y la que usan
  // SubAtendidos y NecesidadRecurrente para sus ventanas (punto 10).
  @NotNull(message = "El formulario requiere la fecha de realización")
  private LocalDate fechaRealizacion;
}