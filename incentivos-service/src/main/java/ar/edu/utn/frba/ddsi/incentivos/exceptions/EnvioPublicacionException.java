package ar.edu.utn.frba.ddsi.incentivos.exceptions;

import ar.edu.utn.frba.ddsi.incentivos.dto.n8n.PerfilPublicacionDTO;
import lombok.Getter;

@Getter
public class EnvioPublicacionException extends RuntimeException{
    private final PerfilPublicacionDTO publicacion;

    public EnvioPublicacionException(PerfilPublicacionDTO publicacion) {
        this.publicacion = publicacion;
    }
}
