package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.SecuenciaCategoriasDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioCategorias;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @param repositorio gestiona la secuencia de categorias existentes en el repositorio
 */
public record GestorCategoria(RepositorioCategorias repositorio) {

    public SecuenciaCategoriasDTO crearCategoria(Categoria nueva) {
        repositorio.obtenerDesdeNivel(nueva.getPosicionSecuencia())
                .forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() + 1));

        repositorio.agregarCategoria(nueva);

        //para retornar al admin
        return new SecuenciaCategoriasDTO(repositorio.obtenerCategoriasOrdenadasPor(Categoria::getPosicionSecuencia));
    }

    public SecuenciaCategoriasDTO eliminarCategoria(UUID idCategoria) {
        Categoria cat = repositorio.buscarPorId(idCategoria);
        repositorio.eliminarCategoria(cat);
        repositorio.obtenerDesdeNivel(cat.getPosicionSecuencia() + 1)
                .forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() - 1));

        //para retornar al admin
        return new SecuenciaCategoriasDTO(repositorio.obtenerCategoriasOrdenadasPor(Categoria::getPosicionSecuencia));
    }

    public Categoria actualizarCategoria(Categoria categoria) {
        if (categoria.getIdCategoria() == null) {
            return null;
        }

        Categoria categoriaActual = repositorio.buscarPorId(categoria.getIdCategoria());
        if (categoriaActual == null) return null;

        if (categoria.getNombre() != null) { //modifica nomCategoria
            categoriaActual.setNombre(categoria.getNombre());
        }

        if (!categoria.getMisiones().isEmpty()) {
            //modifica las misiones de categoria
            //pasame la lista completa con la modificacion
            //hacer que reciba una operacion con una mision de la list es complejo :p
            categoriaActual.setMisiones(categoria.getMisiones());
        }

        if (categoria.getPosicionSecuencia() != null) {
            Integer posicionAnterior = categoriaActual.getPosicionSecuencia();
            Integer posicionNueva = categoria.getPosicionSecuencia();

            if (posicionNueva < 1
                    || posicionNueva > repositorio.obtenerDesdeNivel(1).size()) {
                return null;
            }

            List<Categoria> modificarPosiciones = new ArrayList<>();
            if (posicionNueva < posicionAnterior) {
                // La categoría sube: las que estaban entre ambos lugares bajan un puesto
                modificarPosiciones = repositorio.obtenerDesdeNivel(posicionNueva).stream()
                        .filter(c -> !c.getIdCategoria().equals(categoria.getIdCategoria()))
                        .filter(c -> c.getPosicionSecuencia() < posicionAnterior)
                        .toList();

                modificarPosiciones.forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() + 1));

                for (Categoria x : modificarPosiciones) {
                    repositorio.actualizar(x);
                }
            } else if (posicionNueva > posicionAnterior) {
                // La categoría baja: las que estaban entre ambos lugares suben un puesto
                modificarPosiciones = repositorio.obtenerDesdeNivel(posicionAnterior + 1).stream()
                        .filter(c -> c.getPosicionSecuencia() <= posicionNueva)
                        .toList();

                modificarPosiciones.forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() - 1));

                for (Categoria x : modificarPosiciones) {
                    repositorio.actualizar(x);
                }
            }
            categoriaActual.setPosicionSecuencia(posicionNueva);
        }

        return repositorio.actualizar(categoria);
    }
}
