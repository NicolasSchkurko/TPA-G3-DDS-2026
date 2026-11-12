package ar.edu.utn.frba.ddsi.front.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChoferDTO {
    private UUID idChofer;

    @NotBlank(message = "Indique el nombre del chofer")
    private String nombre;

    private Boolean disponible;

    public ChoferDTO(
            String nombre,
            Boolean disponible) {

        this.nombre = nombre;
        this.disponible = disponible;
    }
}