package ar.edu.utn.frba.ddsi.incentivos.dto.n8n;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerfilPublicacionDTO {
    private String nombreUsuario;
    private String nombreInsignia;
    private String descripcionInsignia;

    public PerfilPublicacionDTO(String nombreUsuario,
                                String nombreInsignia,
                                String descripcionInsignia) {
        this.nombreUsuario = nombreUsuario;
        this.nombreInsignia = nombreInsignia;
        this.descripcionInsignia = descripcionInsignia;
    }
}
