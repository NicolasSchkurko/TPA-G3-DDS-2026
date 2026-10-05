package ar.edu.utn.frba.ddsi.incentivos.dto.Persona;

import lombok.Getter;

/**
 * Lo unico del perfil que el enunciado pide que sea visible publicamente: el nombre de
 * usuario junto al nombre de su categoria actual.
 *
 * <p>Es un DTO aparte y no el {@code PerfilDTO} completo a proposito (punto 8): el endpoint
 * publico es `permitAll()`, asi que lo que devuelva es visible para cualquiera que llegue al
 * servicio. Por eso solo expone estos dos campos y no la mision vigente, las insignias ni
 * los ids internos.
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