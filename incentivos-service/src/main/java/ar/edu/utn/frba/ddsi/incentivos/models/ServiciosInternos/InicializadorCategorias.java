package ar.edu.utn.frba.ddsi.incentivos.models.ServiciosInternos;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioMisiones;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Siembra el programa base de categorías y misiones la primera vez que arranca el
 * servicio, en una base vacía.
 *
 * <p>{@code @Order(1)} lo hace correr primero: las categorías son la raíz de la que cuelga
 * todo lo demás.
 */
@Component
@Order(1)
public class InicializadorCategorias implements CommandLineRunner {

    /**
     * Dónde están publicadas las insignias del seed.
     *
     * <p><b>Es una URL, y no una ruta dentro de este servicio.</b> La insignia guarda la
     * <em>referencia</em> a su imagen, nunca los bytes: el enunciado pide que la insignia
     * tenga una imagen y lo que el dominio tiene que saber es dónde se la puede ver. Dónde
     * vivan los bytes es una decisión del servicio que los publica, y separarla del modelo
     * es lo que permite que mañana ese servicio sea un CDN o un bucket sin tocar nada acá.
     *
     * <p>Es una constante y no una propiedad de configuración porque son datos de ejemplo
     * del programa semilla, no un parámetro del despliegue. Si algún día el servicio expone
     * las insignias, acá pasa a ser su endpoint y el resto no se toca.
     */
    private static final String URL_INSIGNIA = "https://incentivos.example.edu.ar/img/";

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

        // Cada insignia lleva su propio texto (punto 24). Antes el seed no lo daba y la
        // insignia se quedaba con el nombre de la misión como descripción, o sea que el
        // "texto propio de la insignia" era literalmente el título de otra cosa.
        //
        // La imagen es una URL completa y no una ruta: el enunciado pide que la insignia
        // tenga imagen, y lo que el sistema guarda es la referencia a ella. Dónde vivan los
        // bytes es una decisión del servicio que publica las insignias, no del modelo.
        Mision misionPrimera = misionFactory.crearMision(
            null,
            "Primera donación",
            "Realiza tu primera donación para empezar a colaborar.",
            "Primer paso",
            "Otorgada a quien completa su primera donación al programa.",
            URL_INSIGNIA + "primer-paso.png",
            null,
            misionFactory.crearAtributoImpacto("ESTADO"),
            misionFactory.crearOperacion("COINCIDENCIAS", 1, null, "ENTREGADA")
        );
        Mision misionRacha = misionFactory.crearMision(
            null,
            "Racha",
            "Realiza 1 donación durante 3 meses consecutivos.",
            "Constancia solidaria",
            "Reconoce la continuidad: una donación en cada uno de tres meses seguidos.",
            URL_INSIGNIA + "constancia-solidaria.png",
            misionFactory.crearConstancia(1, "MONTHS"),
            misionFactory.crearAtributoImpacto("ESTADO"),
            misionFactory.crearOperacion("COINCIDENCIAS", 3, null, "ENTREGADA")
        );
        Mision misionCompletitud = misionFactory.crearMision(
            null,
            "Completitud",
            "Realiza 5 donaciones de al menos 3 categorías distintas.",
            "Donador versátil",
            "Se otorga al donante que reparte su ayuda entre varias categorías de bienes.",
            URL_INSIGNIA + "donador-versatil.png",
            null,
            misionFactory.crearAtributoImpacto("CATEGORIA"),
            misionFactory.crearOperacion("VALORES_DISTINTOS", 5, 3, null)
        );
        Mision misionHabilDonador = misionFactory.crearMision(
            null,
            "Hábil Donador",
            "Realiza 1 donación que supere 6 bienes.",
            "Manos a la obra",
            "Para la donación generosa: una sola entrega de siete bienes o más.",
            URL_INSIGNIA + "manos-a-la-obra.png",
            null,
            misionFactory.crearAtributoImpacto("CANTIDAD_BIENES"),
            // El umbral es exclusivo: "supera 6" significa 7 o más (punto 33).
            misionFactory.crearOperacion("SUPERA_CANTIDAD", 1, 6, null)
        );
        Mision misionDonacionesExitosas = misionFactory.crearMision(
            null,
            "Donaciones Exitosas",
            "Logra 1 donación con estado recibida por una entidad beneficiaria.",
            "Ayuda recibida",
            "Certifica que la donación llegó efectivamente a la entidad beneficiaria.",
            URL_INSIGNIA + "ayuda-recibida.png",
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
        final Categoria transformador = new Categoria("Transformador", null, 3, new ArrayList<>());

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
