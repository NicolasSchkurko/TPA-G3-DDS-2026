package ar.edu.utn.frba.ddsi.incentivos.dto.Admin;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CategoriaDTO {
    private String nombre;
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