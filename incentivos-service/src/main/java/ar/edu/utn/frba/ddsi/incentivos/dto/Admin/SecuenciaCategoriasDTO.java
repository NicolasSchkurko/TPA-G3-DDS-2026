package ar.edu.utn.frba.ddsi.incentivos.dto.Admin;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public class SecuenciaCategoriasDTO {
    private List<CategoriaDTO> categorias;

    public SecuenciaCategoriasDTO(List<CategoriaDTO> categorias) {
        this.categorias = categorias;
    }
}
