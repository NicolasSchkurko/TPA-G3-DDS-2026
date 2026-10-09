package ar.edu.utn.frba.ddsi.incentivos.dto.Admin;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CategoriaDTO {

    @NotBlank(message = "La categoría requiere un nombre")
    private String nombre;

    /**
     * En qué lugar del programa queda la categoría. Opcional: sin posición queda al final. El
     * límite superior lo revisa {@code SecuenciaCategoria}.
     */
    @Min(value = 1, message = "La posición en la secuencia tiene que ser 1 o más")
    private Integer posicionSecuencia;

    private List<UUID> misiones;

    public CategoriaDTO(String nombre,
                        Integer posicionSecuencia,
                        List<UUID> misiones) {
        this.nombre = nombre;
        this.posicionSecuencia = posicionSecuencia;
        this.misiones = misiones;
    }

    public static CategoriaDTO desdeEntidad(Categoria categoria) {
        if (categoria == null) {
            return null;
        }

        List<UUID> idMisiones = categoria.getCategoriaMisiones().stream()
                                         .map(cm -> cm.getMision().getIdMision())
                                         .toList();

        return new CategoriaDTO(
            categoria.getNombre(),
            categoria.getPosicionSecuencia(),
            idMisiones
        );
    }
}
