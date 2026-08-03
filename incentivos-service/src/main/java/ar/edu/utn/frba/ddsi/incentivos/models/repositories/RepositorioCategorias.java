package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.function.Function;

@Repository
public class RepositorioCategorias {
    private final List<Categoria> categorias;

    public RepositorioCategorias() {
        this.categorias = new ArrayList<>();
    }

    public Categoria buscarPorId(UUID id) {
        if (id == null || categorias.isEmpty()) return null;

        return categorias.stream()
                .filter(c -> id.equals(c.getIdCategoria()))
                .findFirst()
                .orElse(null);
    }

    // Metodo genérico para ordenar cualquier lista por el atributo que le indiquemos
    public <U extends Comparable<? super U>>
    List<Categoria> obtenerCategoriasOrdenadasPor(Function<Categoria, U> keyExtractor) {
        List<Categoria> listaOrdenada = new ArrayList<>(this.categorias);
        listaOrdenada.sort(Comparator.comparing(keyExtractor));
        return listaOrdenada;
    }

    public void agregarCategoria(Categoria categoria) {
        if (!categorias.contains(categoria)) {
            categorias.add(categoria);
        }
    }

    public void eliminarCategoria(Categoria categoria) {
        categorias.remove(categoria);
    }

    public List<Categoria> obtenerDesdeNivel(int nivel) {
        return categorias.stream()
                .filter(c -> c.getPosicionSecuencia() >= nivel)
                .toList();
    }

    public Optional<Categoria> obtenerSiguiente(int nivel) {
        return categorias.stream()
                .filter(c -> c.getPosicionSecuencia() == nivel + 1)
                .findFirst();
    }

    //para modificacion de categorias
    public Categoria actualizar(Categoria categoriaModificada) {
        if (categoriaModificada == null) {
            return null;
        }

        Categoria existente = this.buscarPorId(categoriaModificada.getIdCategoria());

        if (existente != null) {
            int index = categorias.indexOf(existente);
            if (index >= 0) {
                categorias.set(index, existente);
            }
            return existente;
        }

        return null;
    }
}
