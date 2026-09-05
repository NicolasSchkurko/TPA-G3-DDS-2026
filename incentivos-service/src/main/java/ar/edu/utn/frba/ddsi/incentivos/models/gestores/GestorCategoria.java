package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioCategorias;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GestorCategoria {
    private final MisionFactory misionFactory;
    private final RepositorioCategorias repositorio;

    public GestorCategoria(MisionFactory misionFactory, RepositorioCategorias repositorio) {
        this.misionFactory = misionFactory;
        this.repositorio = repositorio;
        this.inicializarCategoriasBase();
    }

    @Transactional
    public List<Categoria> inicializarCategoriasBase() {
        // Solo inicializamos si la tabla de la base de datos está vacía
        if (repositorio.count() == 0) {
            Categoria colaborador = new Categoria("Colaborador", null, 1, new ArrayList<>());

            colaborador.agregarMision(
                misionFactory.crearMision(
                    "Primera donación",
                    "Realiza tu primera donación para empezar a colaborar.",
                    "Primer paso",
                    null,
                    AtributoImpacto.ESTADO,
                    misionFactory.crearOperacion("COINCIDENCIAS", 1, null, "ENTREGADA")
                )
            );

            repositorio.save(colaborador);
            repositorio.save(new Categoria("Sostenedor", null, 2, new ArrayList<>()));
            repositorio.save(new Categoria("Transformador", null, 3, new ArrayList<>()));
        }

        return repositorio.findAllByOrderByPosicionSecuenciaAsc();
    }

    @Transactional
    public List<Categoria> crearCategoria(Categoria nueva) {
        // Desplazamos las siguientes
        List<Categoria> aDesplazar = repositorio.findByPosicionSecuenciaGreaterThanEqual(nueva.getPosicionSecuencia());
        aDesplazar.forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() + 1));

        repositorio.saveAll(aDesplazar);
        repositorio.save(nueva);

        return repositorio.findAllByOrderByPosicionSecuenciaAsc();
    }

    @Transactional
    public List<Categoria> eliminarCategoria(UUID idCategoria) {
        Categoria cat = repositorio.findById(idCategoria).orElse(null);
        if (cat == null) return null;

        Integer posicionLiberada = cat.getPosicionSecuencia();
        repositorio.delete(cat);

        // Acomodamos a las que estaban por debajo para rellenar el hueco
        List<Categoria> aDesplazar = repositorio.findByPosicionSecuenciaGreaterThanEqual(posicionLiberada + 1);
        aDesplazar.forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() - 1));
        repositorio.saveAll(aDesplazar);

        return repositorio.findAllByOrderByPosicionSecuenciaAsc();
    }

    @Transactional
    public Categoria actualizarCategoria(Categoria categoria) {
        if (categoria.getIdCategoria() == null) return null;

        Categoria categoriaActual = repositorio.findById(categoria.getIdCategoria()).orElse(null);
        if (categoriaActual == null) return null;

        if (categoria.getNombre() != null) {
            categoriaActual.setNombre(categoria.getNombre());
        }

        if (categoria.getCategoriaMisiones() != null && !categoria.getCategoriaMisiones().isEmpty()) {
            categoriaActual.setCategoriaMisiones(categoria.getCategoriaMisiones());
        }

        if (categoria.getPosicionSecuencia() != null) {
            Integer posicionAnterior = categoriaActual.getPosicionSecuencia();
            Integer posicionNueva = categoria.getPosicionSecuencia();

            long totalCategorias = repositorio.count();
            if (posicionNueva < 1 || posicionNueva > totalCategorias) {
                return null;
            }

            if (posicionNueva < posicionAnterior) {
                List<Categoria> intermedias = repositorio.findByPosicionSecuenciaBetween(posicionNueva, posicionAnterior - 1);
                intermedias.forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() + 1));
                repositorio.saveAll(intermedias);
            } else if (posicionNueva > posicionAnterior) {
                List<Categoria> intermedias = repositorio.findByPosicionSecuenciaBetween(posicionAnterior + 1, posicionNueva);
                intermedias.forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() - 1));
                repositorio.saveAll(intermedias);
            }
            categoriaActual.setPosicionSecuencia(posicionNueva);
        }

        return repositorio.save(categoriaActual);
    }
}