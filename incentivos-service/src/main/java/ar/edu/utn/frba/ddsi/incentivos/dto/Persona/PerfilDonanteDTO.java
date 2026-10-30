package ar.edu.utn.frba.ddsi.incentivos.dto.Persona;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PerfilDonanteDTO {
    // recibimos de PersonaDonanteDTO-Serv-Donaciones

    @NotNull(message = "El donante requiere un id de usuario")
    private UUID idUsuario;

    @NotBlank(message = "El donante requiere un nombre de usuario")
    @Size(max = 500, message = "El nombre de usuario no puede superar los 500 caracteres")
    private String nombreUsuario;

    private String role;

    public PerfilDonanteDTO(UUID uuid, String nombreUsuario, String role) {
        this.idUsuario = uuid;
        this.nombreUsuario = nombreUsuario;
        this.role = role;
    }
}
