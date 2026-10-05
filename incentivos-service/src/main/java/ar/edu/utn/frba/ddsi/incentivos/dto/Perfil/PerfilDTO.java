package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos de un perfil donante. Se usa como cuerpo de {@code PUT /api/perfiles/{id}} y
 * también como respuesta.
 *
 * <p>No lleva {@code @NotBlank} en los campos a propósito: la actualización es
 * parcial, un campo en null significa "no lo cambies". Si se validara, un cliente
 * que solo quiere cambiar el nombre no podría omitir el resto.
 */
@Getter
@Setter
@NoArgsConstructor
public class PerfilDTO {
    private String nombreUsuario;
    private String categoriaActual;
    private List<String> insignias;
    private String misionActual;

    public PerfilDTO(String nombreUsuario,
                     String categoriaActual,
                     List<String> insignias,
                     String misionActual) {
        this.nombreUsuario = nombreUsuario;
        this.categoriaActual = categoriaActual;
        this.insignias = insignias;
        this.misionActual = misionActual;
    }
}
