package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.TipoCategoria;

import java.util.ArrayList;
import java.util.List;

import lombok.Setter;
import lombok.Getter;

@Getter
@Setter

public class RepositorioCategorias {
    private static RepositorioCategorias instanciaUnica;
    private final List<Categoria> categorias = new ArrayList<>();

    public static RepositorioCategorias getInstance() {
        if (instanciaUnica == null) {
            instanciaUnica = new RepositorioCategorias();
        }
        return instanciaUnica;
    }

    public void guardar(Categoria categoria) {
        categorias.add(categoria);
    }

    public Categoria buscarPorTipo(TipoCategoria tipo) {
        return categorias.stream()
                .filter(categoria -> categoria.getNombre() == tipo)
                .findFirst()
                .orElse(null);
    }
}
