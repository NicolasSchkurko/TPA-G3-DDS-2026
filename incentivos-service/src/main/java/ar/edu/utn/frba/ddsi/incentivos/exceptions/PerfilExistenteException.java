package ar.edu.utn.frba.ddsi.incentivos.exceptions;

import java.util.UUID;
import lombok.Getter;

@Getter
public class PerfilExistenteException extends RuntimeException {
    private final UUID idUsuario;

    public PerfilExistenteException(UUID idUsuario) {
        super("Ya existe un perfil para el usuario " + idUsuario);
        this.idUsuario = idUsuario;
    }
    // No necesita texto interno, el tipo de excepción ya define el error de negocio
}
