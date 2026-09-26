package ar.edu.utn.frba.ddsi.incentivos.models.ServiciosInternos;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioMisiones;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(1)
public class InicializadorCategorias implements CommandLineRunner {
    private final RepositorioCategorias repositorioCategorias;
    private final RepositorioMisiones repositorioMisiones;
    private final MisionFactory misionFactory;

    public InicializadorCategorias(RepositorioCategorias repositorioCategorias,
                                   RepositorioMisiones repositorioMisiones,
                                   MisionFactory misionFactory) {
        this.repositorioCategorias = repositorioCategorias;
        this.repositorioMisiones = repositorioMisiones;
        this.misionFactory = misionFactory;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (repositorioCategorias.count() != 0) {
            return;
        }

        Mision misionPrimera = misionFactory.crearMision(
            null,
            "Primera donación",
            "Realiza tu primera donación para empezar a colaborar.",
            "Primer paso",
            null,
            misionFactory.crearAtributoImpacto("ESTADO"),
            misionFactory.crearOperacion("COINCIDENCIAS", 1, null, "ENTREGADA")
        );
        Mision misionRacha = misionFactory.crearMision(
            null,
            "Racha",
            "Realiza 1 donación durante 3 meses consecutivos.",
            "Constancia solidaria",
            misionFactory.crearConstancia(1, "MONTHS"),
            misionFactory.crearAtributoImpacto("ESTADO"),
            misionFactory.crearOperacion("COINCIDENCIAS", 3, null, "ENTREGADA")
        );
        Mision misionCompletitud = misionFactory.crearMision(
            null,
            "Completitud",
            "Realiza 5 donaciones de al menos 3 categorías distintas.",
            "Donador versátil",
            null,
            misionFactory.crearAtributoImpacto("CATEGORIA"),
            misionFactory.crearOperacion("VALORES_DISTINTOS", 5, 3, null)
        );
        Mision misionHabilDonador = misionFactory.crearMision(
            null,
            "Hábil Donador",
            "Realiza 1 donación que supere 6 bienes.",
            "Manos a la obra",
            null,
            misionFactory.crearAtributoImpacto("CANTIDAD_BIENES"),
            misionFactory.crearOperacion("SUPERA_CANTIDAD", 1, 6, null)
        );
        Mision misionDonacionesExitosas = misionFactory.crearMision(
            null,
            "Donaciones Exitosas",
            "Logra 1 donación con estado recibida por una entidad beneficiaria.",
            "Ayuda recibida",
            null,
            misionFactory.crearAtributoImpacto("ESTADO"),
            misionFactory.crearOperacion("COINCIDENCIAS", 1, null, "RECIBIDA")
        );

        // Guardar primero las misiones creadas
        repositorioMisiones.saveAll(List.of(
            misionPrimera, misionRacha, misionCompletitud,
            misionHabilDonador, misionDonacionesExitosas
        ));

        Categoria colaborador = new Categoria("Colaborador", null, 1, new ArrayList<>());
        Categoria sostenedor = new Categoria("Sostenedor", null, 2, new ArrayList<>());
        Categoria transformador = new Categoria("Transformador", null, 3, new ArrayList<>());

        colaborador.agregarMision(misionPrimera);
        sostenedor.agregarMision(misionRacha);
        sostenedor.agregarMision(misionHabilDonador);
        transformador.agregarMision(misionRacha);
        transformador.agregarMision(misionCompletitud);
        transformador.agregarMision(misionHabilDonador);
        transformador.agregarMision(misionDonacionesExitosas);

        repositorioCategorias.saveAll(List.of(colaborador, sostenedor, transformador));
    }
}