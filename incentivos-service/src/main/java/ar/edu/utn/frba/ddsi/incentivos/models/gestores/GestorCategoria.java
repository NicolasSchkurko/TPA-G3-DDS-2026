package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.events.CategoriaCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.events.UltimaMisionCategoria;
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
    private final ApplicationEventPublisher eventPublisher;
    private final MisionFactory misionFactory;

    public GestorCategoria(RepositorioCategorias repositorio,
                           ApplicationEventPublisher eventPublisher,
                           MisionFactory misionFactory) {
        this.repositorio = repositorio;
        this.eventPublisher = eventPublisher;
        this.misionFactory = misionFactory;
        this.inicializarCategoriasBase();
    }

    @EventListener
    public void avanzarCategoria(UltimaMisionCategoria event) {
        if (event.idCategoriaCompletada() == null) {
            return;
        }

        Categoria categoriaAnterior = repositorio.buscarPorId(event.idCategoriaCompletada());
        Categoria categoriaSiguiente = repositorio.buscarPorPosicionSecuencia(categoriaAnterior.getPosicionSecuencia() + 1);
        if (categoriaSiguiente == null || categoriaSiguiente.primeraMision() == null) {
            return;
        }

        eventPublisher.publishEvent(
                new CategoriaCambiada(
                        categoriaAnterior,
                        categoriaSiguiente,
                        event.idPerfil()
                )
        );
    }

    //init default, dsp el admin puede modificarlas
    public List<Categoria> inicializarCategoriasBase() {
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

        repositorio.agregarCategoria(colaborador);
        repositorio.agregarCategoria(new Categoria("Sostenedor", 2, new ArrayList<>()));
        repositorio.agregarCategoria(new Categoria("Transformador", 3, new ArrayList<>()));

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
}
