package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas.TipoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.UUID;

@Repository
public class RepositorioCategorias {
    private final List<Categoria> categorias;

    public RepositorioCategorias() {
        this.categorias = new ArrayList<>();
        inicializarCategoriasBase();
    }

    private void inicializarCategoriasBase() {
        this.categorias.add(new Categoria("Colaborador", 1, new ArrayList<>() ));
        this.categorias.add(new Categoria("Sostenedor", 2, new ArrayList<>() ));
        this.categorias.add(new Categoria("Transformador", 3, new ArrayList<>() ));
        this.categorias.getFirst().agregarMision(MisionFactory.crearMision(TipoMision.COMPLETITUD));
    }

    public Categoria buscarPorId(UUID id) {
        if (id == null || categorias.isEmpty()) return null;

        return categorias.stream()
                .filter(c -> id.equals(c.getIdCategoria()))
                .findFirst()
                .orElse(null);
    }

    //TODO poder modificar categorias

    // Método genérico para ordenar cualquier lista por el atributo que le indiquemos
    public <U extends Comparable<? super U>> List<String> obtenerCategoriasOrdenadasPor(Function<Categoria, U> keyExtractor) {
        List<Categoria> listaOrdenada = new ArrayList<>(this.categorias);
        listaOrdenada.sort(Comparator.comparing(keyExtractor));
        return listaOrdenada.stream().map(Categoria::getNombre).toList();
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

    public Categoria obtenerSiguiente(int nivel) {
        return categorias.get(nivel + 1);
    }
}
