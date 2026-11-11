package ar.edu.utn.frba.ddsi.front.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CamionDTO {
    private UUID idChofer;

    @NotBlank(message = "es obligatorio")
    private String patente;

    @NotNull(message = "es obligatorio")
    private Double capacidadVolumen; //m2

    @NotNull(message = "es obligatorio")
    private Double altura; //m

    @NotNull(message = "es obligatorio")
    private Double capacidadCarga;

    private Boolean disponible;
}