package ar.edu.utn.frba.ddsi.incentivos.exceptions;

import java.util.UUID;
import lombok.Getter;

/**
 * El usuario ya tiene un perfil, así que no se puede crear otro. Se traduce a 409.
 */
@Getter
public class PerfilExistenteException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final UUID idUsuario;

    public PerfilExistenteException(UUID idUsuario) {
        super("Ya existe un perfil para el usuario " + idUsuario);
        this.idUsuario = idUsuario;
    }
}
