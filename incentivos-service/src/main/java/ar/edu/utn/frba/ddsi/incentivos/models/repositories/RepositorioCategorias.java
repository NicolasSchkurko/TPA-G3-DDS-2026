package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.TipoCategoria;

import java.util.ArrayList;
import java.util.List;

public class RepositorioCategorias {
    private static RepositorioCategorias instance = null;
    private final List<Categoria> categorias;

    private RepositorioCategorias() {
        this.categorias = new ArrayList<>();
        inicializarCategoriasBase();
    }

    public static synchronized RepositorioCategorias getInstance() {
        if (instance == null) {
            instance = new RepositorioCategorias();
        }
        return instance;
    }

    private void inicializarCategoriasBase() {
        this.categorias.add(new Categoria(TipoCategoria.COLABORADOR, TipoCategoria.SOSTENEDOR));
        this.categorias.add(new Categoria(TipoCategoria.SOSTENEDOR, TipoCategoria.TRANSFORMADOR));
        this.categorias.add(new Categoria(TipoCategoria.TRANSFORMADOR, TipoCategoria.TRANSFORMADOR));
    }

    public Categoria buscarPorTipo(TipoCategoria tipo) {
        if (tipo == null) return null;
        return this.categorias.stream()
                .filter(c -> c.getNombre() == tipo)
                .findFirst()
                .orElse(null);
    }

    public List<Categoria> obtenerTodas() {
        return new ArrayList<>(this.categorias);
    }
}
