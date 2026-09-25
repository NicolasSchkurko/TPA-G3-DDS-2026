package ar.edu.utn.frba.ddsi.incentivos.models.ServiciosInternos;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorMision;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(1)
//Crea las misiones y categorías base al iniciar la aplicación si todavía no existen.
public class InicializadorCategorias implements CommandLineRunner {
    private final RepositorioCategorias repositorioCategorias;
    private final GestorMision gestorMisiones;

    public InicializadorCategorias(RepositorioCategorias repositorioCategorias,
                                   GestorMision gestorMisiones) {
        this.repositorioCategorias = repositorioCategorias;
        this.gestorMisiones = gestorMisiones;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (repositorioCategorias.count() != 0) {
            return;
        }

        Mision misionPrimera = gestorMisiones.crearMision(
                null,
                "Primera donación",
                "Realiza tu primera donación para empezar a colaborar.",
                "Primer paso",
                null,
                "ESTADO",
                gestorMisiones.conseguirOperacion("COINCIDENCIAS", 1, null, "ENTREGADA")
        );
        Mision misionRacha = gestorMisiones.crearMision(
                null,
                "Racha",
                "Realiza 1 donación durante 3 meses consecutivos.",
                "Constancia solidaria",
                gestorMisiones.conseguirConstancia(1, "MONTHS"),
                "ESTADO",
                gestorMisiones.conseguirOperacion("COINCIDENCIAS", 3, null, "ENTREGADA")
        );
        Mision misionCompletitud = gestorMisiones.crearMision(
                null,
                "Completitud",
                "Realiza 5 donaciones de al menos 3 categorías distintas.",
                "Donador versátil",
                null,
                "CATEGORIA",
                gestorMisiones.conseguirOperacion("VALORES_DISTINTOS", 5, 3, null)
        );
        Mision misionHabilDonador = gestorMisiones.crearMision(
                null,
                "Hábil Donador",
                "Realiza 1 donación que supere 6 bienes.",
                "Manos a la obra",
                null,
                "CANTIDAD_BIENES",
                gestorMisiones.conseguirOperacion("SUPERA_CANTIDAD", 1, 6, null)
        );
        Mision misionDonacionesExitosas = gestorMisiones.crearMision(
                null,
                "Donaciones Exitosas",
                "Logra 1 donación con estado recibida por una entidad beneficiaria.",
                "Ayuda recibida",
                null,
                "ESTADO",
                gestorMisiones.conseguirOperacion("COINCIDENCIAS", 1, null, "RECIBIDA")
        );

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
