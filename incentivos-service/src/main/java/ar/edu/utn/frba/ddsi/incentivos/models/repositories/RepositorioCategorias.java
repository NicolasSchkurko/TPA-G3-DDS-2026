package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import java.util.List;
import org.springdoc.core.converters.models.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioCategorias
        extends JpaRepository<Categoria, UUID>{

    Optional<Categoria> findByIdCategoria(UUID idCategoria);

    // Ejemplo con JPQL personalizado:
    @Query("SELECT c FROM Categoria c WHERE c.posicionSecuencia = :posicion")
    Optional<Categoria> buscarPorPosicionSecuencia(@Param("posicion") int posicion);

//    // Metodo genérico para ordenar cualquier lista por el atributo que le indiquemos
//    public <U extends Comparable<? super U>>
//    List<Categoria> obtenerCategoriasOrdenadasPor
//    (Function<Categoria, U> keyExtractor) {
//        List<Categoria> listaOrdenada = new ArrayList<>(this.categorias);
//        listaOrdenada.sort(Comparator.comparing(keyExtractor));
//        return listaOrdenada;
//    }
//
//    public void eliminarCategoria(Categoria categoria) {
//        categorias.remove(categoria);
//    }
//
//    public List<Categoria> obtenerDesdeNivel(int nivel) {
//        return categorias.stream()
//                .filter(c -> c.getPosicionSecuencia() >= nivel)
//                .toList();
//    }
//
//    //para modificacion de categorias
//    public Categoria actualizar(Categoria categoriaModificada) {
//        if (categoriaModificada == null) {
//            return null;
//        }
//
//        Categoria existente = this.buscarPorId(categoriaModificada.getIdCategoria());
//
//        if (existente != null) {
//            int index = categorias.indexOf(existente);
//            if (index >= 0) {
//                categorias.set(index, existente);
//            }
//            return existente;
//        }
//
//        return null;
//    }
}
