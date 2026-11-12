package ar.edu.utn.frba.ddsi.incentivos.dto.n8n;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerfilPublicacionDTO {
    private String prompt;
    private String mensaje;
    private String nomUsuario;
    private UUID idUsuario;
    private UUID idInsignia;

    public PerfilPublicacionDTO(String prompt,
                                String mensaje,
                                String nomUsuario,
                                UUID idUsuario,
                                UUID idInsignia) {
        this.prompt = prompt;
        this.mensaje = mensaje;
        this.nomUsuario = nomUsuario;
        this.idUsuario = idUsuario;
        this.idInsignia = idInsignia;
    }

    public PerfilPublicacionDTO(String prompt, String mensaje, String redSocial,
                                String nomUsuario, UUID idUsuario) {
        this(prompt, mensaje, redSocial, nomUsuario, idUsuario, null);
    }
}
