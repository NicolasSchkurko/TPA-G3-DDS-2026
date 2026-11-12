package ar.edu.utn.frba.ddsi.front.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChoferDTO {
    private UUID idChofer;
    private String nombre;
    private String email;
    private Boolean disponible;
    private Boolean activo;

    public boolean estaActivo() {
        return activo == null || activo;
    }
}
