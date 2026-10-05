package ar.edu.utn.frba.ddsi.incentivos.dto.Persona;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class PerfilDonanteDTO {
    //recibimos de PersonaDonanteDTO-Serv-Donaciones

    @NotNull(message = "El donante requiere un id de usuario")
    private UUID idUsuario;

    @NotBlank(message = "El donante requiere un nombre de usuario")
    private String nombreUsuario;

    private String role;

    public PerfilDonanteDTO(UUID uuid, String nombreUsuario, String role) {
        this.idUsuario = uuid;
        this.nombreUsuario = nombreUsuario;
        this.role = role;
    }
}
