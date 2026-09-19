package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioMisiones;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GestorCategoria {
    private final MisionFactory misionFactory;
    private final RepositorioCategorias repositorio;
    private final RepositorioMisiones repositorioMisiones;

    public GestorCategoria(MisionFactory misionFactory,
                           RepositorioCategorias repositorio,
                           RepositorioMisiones repositorioMisiones) {
        this.misionFactory = misionFactory;
        this.repositorio = repositorio;
        this.repositorioMisiones = repositorioMisiones;
    }

    @Transactional
    public void inicializarCategoriasBase() {
        // Solo inicializamos si la tabla de la base de datos está vacía
        if (repositorio.count() == 0) {
            // Crear misiones base
            Mision mision1 = misionFactory.crearMision(
                null,
                "Primera donación",
                "Realiza tu primera donación para empezar a colaborar.",
                "Primer paso",
                null,
                AtributoImpacto.ESTADO,
                misionFactory.crearOperacion("COINCIDENCIAS", 1, null, "ENTREGADA")
            );

            Mision mision2 = misionFactory.crearMision(
                null,
                "Segunda donación",
                "Realiza tu segunda donación.",
                "Sigo ayudando",
                null,
                AtributoImpacto.ESTADO,
                misionFactory.crearOperacion("COINCIDENCIAS", 1, null, "ENTREGADA")
            );

            Mision mision3 = misionFactory.crearMision(
                null,
                "Supera tus límites",
                "Dona más de 10 bienes",
                "Rompiendo los límites",
                null,
                AtributoImpacto.CANTIDAD_BIENES,
                misionFactory.crearOperacion("SUPERA_CANTIDAD", 1, 10, "ENTREGADA")
            );

            repositorioMisiones.saveAll(List.of(mision1, mision2, mision3));

            // Crear categorías base
            Categoria colaborador = new Categoria("Colaborador", null, 1, new ArrayList<>());
            Categoria sostenedor = new Categoria("Sostenedor", null, 2, new ArrayList<>());
            Categoria transformador = new Categoria("Transformador", null, 3, new ArrayList<>());

            colaborador.agregarMision(mision1);
            sostenedor.agregarMision(mision2);
            sostenedor.agregarMision(mision3);

            repositorio.saveAll(List.of(colaborador, sostenedor, transformador));
        }
    }

    // ========== GET ==========
    public List<Categoria> obtenerTodas() {
        return repositorio.findAllByOrderByPosicionSecuenciaAsc();
    }

    public Categoria obtenerPorId(UUID id) {
        return repositorio.findById(id).orElse(null);
    }

    public Categoria obtenerCategoriaSiguiente(Categoria categoriaActual) {
        if (categoriaActual == null || categoriaActual.getPosicionSecuencia() == null) {
            return null;
        }

        return repositorio.findAllByOrderByPosicionSecuenciaAsc().stream()
                          .filter(categoria -> categoria.getPosicionSecuencia() != null)
                          .filter(categoria -> categoria.getPosicionSecuencia() > categoriaActual.getPosicionSecuencia())
                          .findFirst()
                          .orElse(null);
    }

    // ========== CREATE ==========
    @Transactional
    public Categoria crearCategoria(Categoria nueva) {
        // Desplazamos las siguientes categorías
        List<Categoria> aDesplazar = repositorio.findByPosicionSecuenciaGreaterThanEqual(nueva.getPosicionSecuencia());
        aDesplazar.forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() + 1));

        repositorio.saveAll(aDesplazar);
        return repositorio.save(nueva);
    }

    // ========== UPDATE ==========
    @Transactional
    public Categoria actualizarCategoria(Categoria categoria) {
        if (categoria.getIdCategoria() == null) {
            return null;
        }

        Categoria categoriaActual = repositorio.findById(categoria.getIdCategoria()).orElse(null);
        if (categoriaActual == null) {
            return null;
        }

        // Actualizar nombre
        if (categoria.getNombre() != null) {
            categoriaActual.setNombre(categoria.getNombre());
        }

        // Actualizar misiones
        if (categoria.getCategoriaMisiones() != null && !categoria.getCategoriaMisiones().isEmpty()) {
            categoriaActual.setCategoriaMisiones(categoria.getCategoriaMisiones());
        }

        // Actualizar posición (con reordenamiento automático)
        if (categoria.getPosicionSecuencia() != null) {
            Integer posicionAnterior = categoriaActual.getPosicionSecuencia();
            Integer posicionNueva = categoria.getPosicionSecuencia();

            long totalCategorias = repositorio.count();
            if (posicionNueva < 1 || posicionNueva > totalCategorias) {
                return null;
            }

            if (posicionNueva < posicionAnterior) {
                // Subir de posición: desplazar hacia abajo
                List<Categoria> intermedias = repositorio.findByPosicionSecuenciaBetween(posicionNueva, posicionAnterior - 1);
                intermedias.forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() + 1));
                repositorio.saveAll(intermedias);
            } else if (posicionNueva > posicionAnterior) {
                // Bajar de posición: desplazar hacia arriba
                List<Categoria> intermedias = repositorio.findByPosicionSecuenciaBetween(posicionAnterior + 1, posicionNueva);
                intermedias.forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() - 1));
                repositorio.saveAll(intermedias);
            }

            categoriaActual.setPosicionSecuencia(posicionNueva);
        }

        return repositorio.save(categoriaActual);
    }

    // ========== DELETE ==========
    @Transactional
    public List<Categoria> eliminarCategoria(UUID idCategoria) {
        Categoria cat = repositorio.findById(idCategoria).orElse(null);
        if (cat == null) {
            return List.of();
        }

        Integer posicionLiberada = cat.getPosicionSecuencia();
        repositorio.delete(cat);

        // Acomodar categorías por debajo para rellenar el hueco
        List<Categoria> aDesplazar = repositorio.findByPosicionSecuenciaGreaterThanEqual(posicionLiberada + 1);
        aDesplazar.forEach(c -> c.setPosicionSecuencia(c.getPosicionSecuencia() - 1));
        repositorio.saveAll(aDesplazar);

        return repositorio.findAllByOrderByPosicionSecuenciaAsc();
    }
}