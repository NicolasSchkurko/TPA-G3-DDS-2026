package ar.edu.utn.frba.ddsi.incentivos.dto.Persona;

import lombok.Getter;

/**
 * Lo único del perfil visible públicamente: nombre de usuario y categoría. Es un DTO acotado
 * porque el endpoint es {@code permitAll()}.
 */
@Getter
public class PerfilPublicoDTO {

    private final String nombreUsuario;
    private final String nombreCategoria;

    public PerfilPublicoDTO(String nombreUsuario, String nombreCategoria) {
        this.nombreUsuario = nombreUsuario;
        this.nombreCategoria = nombreCategoria;
    }
}
