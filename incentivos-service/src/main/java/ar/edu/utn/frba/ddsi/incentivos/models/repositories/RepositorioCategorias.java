package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.TipoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.TipoCategoria;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class RepositorioCategorias {
    private final List<Categoria> categorias;

    public RepositorioCategorias() {
        this.categorias = new ArrayList<>();
        inicializarCategoriasBase();
    }

    private void inicializarCategoriasBase() {
        this.categorias.add(new Categoria(TipoCategoria.COLABORADOR, TipoCategoria.SOSTENEDOR));
        this.categorias.add(new Categoria(TipoCategoria.SOSTENEDOR, TipoCategoria.TRANSFORMADOR));
        this.categorias.add(new Categoria(TipoCategoria.TRANSFORMADOR, TipoCategoria.TRANSFORMADOR));
        this.categorias.getFirst().agregarMision(MisionFactory.crearMision(TipoMision.COMPLETITUD));
    }

    public Categoria buscarPorTipo(TipoCategoria tipo) {
        if (tipo == null) return null;
        return this.categorias.stream()
                .filter(c -> c.getNombre() == tipo)
                .findFirst()
                .orElse(null);
    }

    //TODO poder crear/modificar categorias

    public List<Categoria> obtenerTodas() {
        return new ArrayList<>(this.categorias);
    }
}
