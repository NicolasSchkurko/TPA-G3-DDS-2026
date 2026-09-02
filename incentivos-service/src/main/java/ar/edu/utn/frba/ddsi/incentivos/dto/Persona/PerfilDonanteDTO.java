package ar.edu.utn.frba.ddsi.incentivos.dto.Persona;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter

public class PerfilDonanteDTO {
    //recibimos de PersonaDonanteDTO-Serv-Donaciones
    private UUID idUsuario;
    private String nombreUsuario;
    private String role;

    public PerfilDonanteDTO(UUID uuid, String nombreUsuario, String role) {
        this.idUsuario = uuid;
        this.nombreUsuario = nombreUsuario;
        this.role = role;
    }
}
