package ar.edu.utn.frba.ddsi.incentivos.dto.n8n;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PerfilPublicacionDTO {
    private String prompt;
    private String mensaje;
    private String redSocial;
    private String nomUsuario;
    private UUID idUsuario;

    public PerfilPublicacionDTO(String prompt,
                                String mensaje,
                                String redSocial,
                                String nomUsuario,
                                UUID idUsuario) {
        this.prompt = prompt;
        this.mensaje = mensaje;
        this.redSocial = redSocial;
        this.nomUsuario = nomUsuario;
        this.idUsuario = idUsuario;
    }
}
