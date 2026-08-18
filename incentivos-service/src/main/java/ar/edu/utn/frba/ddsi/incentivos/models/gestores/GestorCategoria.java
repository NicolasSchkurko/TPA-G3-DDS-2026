package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.events.CategoriaCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.events.MisionCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.events.UltimaMisionCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioCategorias;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GestorCategoria {
    private final RepositorioCategorias repositorio;
    private final GestorPerfiles gestorPerfiles;
    private final ApplicationEventPublisher eventPublisher;

    public GestorCategoria(RepositorioCategorias repositorio,
                           GestorPerfiles gestorPerfiles,
                           ApplicationEventPublisher eventPublisher) {
        this.repositorio = repositorio;
        this.gestorPerfiles = gestorPerfiles;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    public void avanzarCategoria(UltimaMisionCategoria event) {
        if (event.perfil() == null || event.perfil().getCategoriaActual() == null
                || !event.perfil().getCategoriaActual().getIdCategoria()
                .equals(event.idCategoriaCompletada())) {
            return;
        }

        Categoria categoriaAnterior = event.perfil().getCategoriaActual();
        Categoria categoriaSiguiente = categoriaCorrespondiente(categoriaAnterior.getPosicionSecuencia() + 1);
        if (categoriaSiguiente == null || categoriaSiguiente.primeraMision() == null) {
            return;
        }

        Mision misionAnterior = event.perfil().getMisionActual();
        Mision misionNueva = categoriaSiguiente.primeraMision();
        event.perfil().setCategoriaActual(categoriaSiguiente);
        event.perfil().setMisionActual(misionNueva);
        Perfil p = gestorPerfiles.actualizarPerfil(event.perfil());

        eventPublisher.publishEvent(
                new CategoriaCambiada(
                        categoriaAnterior.getNombre(),
                        categoriaSiguiente.getNombre(),
                        p.getNombreUsuario()
                )
        );
        eventPublisher.publishEvent(
                new MisionCambiada(
                        misionAnterior.getNombreMision(),
                        misionAnterior.getInsigniaObjetivo().getNombre(),
                        p.getNombreUsuario(),
                        p.getMisionActual().getNombreMision()
                )
        );
    }

    //init default, dsp el admin puede modificarlas
    public List<Categoria> inicializarCategoriasBase(){
        repositorio.agregarCategoria(new Categoria("Colaborador", 1, new ArrayList<>()) );
        repositorio.agregarCategoria(new Categoria("Sostenedor", 2, new ArrayList<>()) );
        repositorio.agregarCategoria(new Categoria("Transformador", 3, new ArrayList<>()) );

        return repositorio.obtenerCategoriasOrdenadasPor(Categoria::getPosicionSecuencia);
    }

    public List<Categoria> crearCategoria(Categoria nueva) {
        repositorio.obtenerDesdeNivel(nueva.getPosicionSecuencia())
                .forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() + 1));

        repositorio.agregarCategoria(nueva);

        //para retornar al admin
        return repositorio.obtenerCategoriasOrdenadasPor(Categoria::getPosicionSecuencia);
    }

    public List<Categoria> eliminarCategoria(UUID idCategoria) {
        Categoria cat = repositorio.buscarPorId(idCategoria);
        repositorio.eliminarCategoria(cat);
        repositorio.obtenerDesdeNivel(cat.getPosicionSecuencia() + 1)
                .forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() - 1));

        //para retornar al admin
        return repositorio.obtenerCategoriasOrdenadasPor(Categoria::getPosicionSecuencia);
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

        return repositorio.actualizar(categoriaActual);
    }

    public Categoria categoriaCorrespondiente(Integer posicion){
        return repositorio.buscarPorPosicionSecuencia(posicion);
    }
}
