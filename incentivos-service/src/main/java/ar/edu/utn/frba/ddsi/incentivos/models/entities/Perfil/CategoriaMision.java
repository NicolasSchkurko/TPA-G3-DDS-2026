package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoriaMision {
    private Categoria categoria;
    private Mision mision;
    private Integer posicion;

    public CategoriaMision(Categoria categoria, Mision mision, Integer posicion) {
        this.categoria = categoria;
        this.mision = mision;
        this.posicion = posicion;
    }
}
