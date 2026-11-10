package ar.edu.utn.frba.ddsi.logisticas.dto.chofer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ChoferesDTO {
    @NotNull(message = "es obligatorio")
    @Valid
    private List<@NotNull(message = "es obligatorio") ChoferDTO> choferes;

    public ChoferesDTO() {
    }

    public ChoferesDTO(List<ChoferDTO> choferes){
        this.choferes = choferes;
    }
}