package ar.edu.utn.frba.ddsi.logisticas.dto.camion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class CamionesDTO {
    @NotNull(message = "es obligatorio")
    @Valid
    private List<@NotNull(message = "es obligatorio") CamionDTO> camiones;

    public CamionesDTO() {
    }

    public CamionesDTO(List<CamionDTO> camiones){
        this.camiones = camiones;
    }
}
