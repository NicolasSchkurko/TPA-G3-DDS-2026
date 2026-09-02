package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.function.Function;

@Repository
public class RepositorioCategorias {
    private final List<Categoria> categorias;
    private final MisionFactory misionFactory;

    public RepositorioCategorias(MisionFactory misionFactory) {
        this.categorias = new ArrayList<>();
        this.misionFactory=misionFactory;
        this.inicializarCategoriasBase();
    }

    public Categoria buscarPorId(UUID id) {
        if (id == null || categorias.isEmpty()) return null;

        return categorias.stream()
                .filter(c -> id.equals(c.getIdCategoria()))
                .findFirst()
                .orElse(null);
    }

    public Categoria buscarPorPosicionSecuencia(int nivel){
        if(categorias.isEmpty()) return null;

        return categorias.stream()
                .filter(categoria -> categoria.getPosicionSecuencia() == nivel)
                .findFirst().orElse(null);
    }

    // Metodo genérico para ordenar cualquier lista por el atributo que le indiquemos
    public <U extends Comparable<? super U>>
    List<Categoria> obtenerCategoriasOrdenadasPor
    (Function<Categoria, U> keyExtractor) {
        List<Categoria> listaOrdenada = new ArrayList<>(this.categorias);
        listaOrdenada.sort(Comparator.comparing(keyExtractor));
        return listaOrdenada;
    }

    private void agregarCategoria(Categoria categoria) {
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

    //para modificacion de categorias
    public Categoria actualizar(Categoria categoriaModificada) {
        if (categoriaModificada.getIdCategoria() == null) {
            return null;
        }

        Categoria categoriaActual = buscarPorId(categoriaModificada.getIdCategoria());
        if (categoriaActual == null) return null;

        if (categoriaModificada.getNombre() != null) { //modifica nomCategoria
            categoriaActual.setNombre(categoriaModificada.getNombre());
        }

        if (!categoriaModificada.getMisiones().isEmpty()) {
            //modifica las misiones de categoria
            //pasame la lista completa con la modificacion
            //hacer que reciba una operacion con una mision de la list es complejo :p
            categoriaActual.setMisiones(categoriaModificada.getMisiones());
        }

        if (categoriaModificada.getPosicionSecuencia() != null) {
            Integer posicionAnterior = categoriaActual.getPosicionSecuencia();
            Integer posicionNueva = categoriaModificada.getPosicionSecuencia();

            if (posicionNueva < 1
                    || posicionNueva > obtenerDesdeNivel(1).size()) {
                return null;
            }

            List<Categoria> modificarPosiciones = new ArrayList<>();
            if (posicionNueva < posicionAnterior) {
                // La categoría sube: las que estaban entre ambos lugares bajan un puesto
                modificarPosiciones = obtenerDesdeNivel(posicionNueva).stream()
                        .filter(c -> !c.getIdCategoria().equals(categoriaModificada.getIdCategoria()))
                        .filter(c -> c.getPosicionSecuencia() < posicionAnterior)
                        .toList();

                modificarPosiciones.forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() + 1));

                for (Categoria x : modificarPosiciones) {
                    actualizar(x);
                }
            } else if (posicionNueva > posicionAnterior) {
                // La categoría baja: las que estaban entre ambos lugares suben un puesto
                modificarPosiciones = obtenerDesdeNivel(posicionAnterior + 1).stream()
                        .filter(c -> c.getPosicionSecuencia() <= posicionNueva)
                        .toList();

                modificarPosiciones.forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() - 1));

                for (Categoria x : modificarPosiciones) {
                    actualizar(x);
                }
            }
            categoriaActual.setPosicionSecuencia(posicionNueva);
        }

        int index = categorias.indexOf(categoriaActual);
        if (index >= 0) {
            categorias.set(index, categoriaActual);
        }

        return categoriaActual;
    }



    private List<Categoria> inicializarCategoriasBase(){
        Categoria colaborador = new Categoria("Colaborador", 1, new ArrayList<>());

        colaborador.getMisiones().add(
                misionFactory.crearMision(
                        "Primera donación",
                        "Realiza tu primera donación para empezar a colaborar.",
                        "Primer paso",
                        null,
                        AtributoImpacto.ESTADO,
                        misionFactory.crearOperacion("COINCIDENCIAS", 1, null, "ENTREGADA")
                )
        );

        agregarCategoria(colaborador);
        agregarCategoria(new Categoria("Sostenedor", 2, new ArrayList<>()));
        agregarCategoria(new Categoria("Transformador", 3, new ArrayList<>()));

        return obtenerCategoriasOrdenadasPor(Categoria::getPosicionSecuencia);
    }

    public List<Categoria> crearCategoria(Categoria nueva) {
        obtenerDesdeNivel(nueva.getPosicionSecuencia())
                .forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() + 1));

        agregarCategoria(nueva);

        //para retornar al admin
        return obtenerCategoriasOrdenadasPor(Categoria::getPosicionSecuencia);
    }

    public List<Categoria> eliminarCategoriaPorId(UUID idCategoria) {
        Categoria cat = buscarPorId(idCategoria);
        eliminarCategoria(cat);
        obtenerDesdeNivel(cat.getPosicionSecuencia() + 1)
                    .forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() - 1));

        //para retornar al admin
        return obtenerCategoriasOrdenadasPor(Categoria::getPosicionSecuencia);
    }
}
