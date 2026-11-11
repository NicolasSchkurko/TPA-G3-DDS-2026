package ar.edu.utn.frba.ddsi.front.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FormularioDonacionDTO {

    private UUID idDonante;

    @NotNull(message = "Indicá la fecha de realización.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaRealizacion = LocalDate.now();

    @NotEmpty(message = "Agregá al menos un bien a la donación.")
    @Valid
    private List<BienDonacionDTO> bienes = new ArrayList<>(List.of(new BienDonacionDTO()));
}
