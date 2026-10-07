package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.UnidadTiempo;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Arma una {@link Mision} completa a partir de los campos sueltos que manda el admin.
 *
 * <p>Existe para que el controller y el servicio no tengan que saber qué hace falta para
 * dejar una misión consistente: regla de constancia, atributo, tipo de operación y los
 * parámetros de cada uno. También es el lugar donde se valida el texto que viene del JSON,
 * que es texto libre.
 */
@Component
public class MisionFactory {
    private final OperacionFactory operacionFactory;

    public MisionFactory(OperacionFactory operacionFactory) {
        this.operacionFactory = operacionFactory;
    }

    /** Nombres en español de las unidades de tiempo, en el orden en que se documentan. */
    private static final List<String> UNIDADES_EN_ESPANOL = List.of(
        "MINUTOS", "HORAS", "DIAS", "SEMANAS", "MESES", "ANOS"
    );

    /**
     * Traduce el nombre de unidad. Se aceptan los nombres en español y los identificadores
     * en inglés que ya se persistían.
     *
     * <p>El comentario de {@link ReglaConstancia} ya declaraba que la ventana temporal
     * iba de minutos a años, pero el código hacía {@code ChronoUnit.valueOf} sobre
     * cualquier texto y aceptaba {@code FOREVER} o {@code NANOS}, que no significan
     * nada para una racha de donaciones.
     */
    private static final Map<String, UnidadTiempo> UNIDADES_PERMITIDAS = unidadesPermitidas();

    private static Map<String, UnidadTiempo> unidadesPermitidas() {
        Map<String, UnidadTiempo> unidades = new LinkedHashMap<>();

        unidades.put("MINUTOS", UnidadTiempo.MINUTES);
        unidades.put("HORAS", UnidadTiempo.HOURS);
        unidades.put("DIAS", UnidadTiempo.DAYS);
        unidades.put("SEMANAS", UnidadTiempo.WEEKS);
        unidades.put("MESES", UnidadTiempo.MONTHS);
        unidades.put("ANOS", UnidadTiempo.YEARS);

        unidades.put("MINUTES", UnidadTiempo.MINUTES);
        unidades.put("HOURS", UnidadTiempo.HOURS);
        unidades.put("DAYS", UnidadTiempo.DAYS);
        unidades.put("WEEKS", UnidadTiempo.WEEKS);
        unidades.put("MONTHS", UnidadTiempo.MONTHS);
        unidades.put("YEARS", UnidadTiempo.YEARS);

        return Map.copyOf(unidades);
    }

    /**
     * Arma la misión completa desde los campos sueltos del DTO del admin.
     *
     * <p>La constancia es opcional: si no viene ninguna de las dos partes, la misión no exige
     * ventana temporal. Si viene solo una, es un error y no una ausencia, porque "3" sin
     * unidad no significa nada (punto 34).
     *
     * <p>{@code descripcionInsignia} y {@code urlImagenInsignia} son los datos propios de la
     * insignia y van separados de {@code descripcion}, que es el de la misión (punto 24).
     * Aceptan null porque el enunciado pide los tres datos de la insignia pero no todos los
     * clientes los mandan.
     *
     * @throws DatosInvalidosException si algún texto no corresponde a un valor válido, o
     *                               si falta la mitad de la ventana temporal.
     */
    public Mision crearMision(
        UUID idAdmin,
        String nombreMision,
        String descripcion,
        String nombreInsignia,
        String descripcionInsignia,
        String urlImagenInsignia,
        Integer cantidadTiempo,
        String unidadTiempo,
        String atributo,
        String tipoOperacion,
        Integer progresoObjetivo,
        Integer cantidadOperacion,
        String valorEsperado
    ) {
        ReglaConstancia constancia = crearConstancia(cantidadTiempo, unidadTiempo);
        AtributoImpacto atributoImpacto = crearAtributoImpacto(atributo);
        Operacion operacion = crearOperacion(
            tipoOperacion,
            progresoObjetivo,
            cantidadOperacion,
            valorEsperado
        );

        return crearMision(
            idAdmin,
            nombreMision,
            descripcion,
            nombreInsignia,
            descripcionInsignia,
            urlImagenInsignia,
            constancia,
            atributoImpacto,
            operacion
        );
    }

    /**
     * La sobrecarga para quien solo tiene el nombre de la insignia y nada mas.
     *
     * <p>Existe para no obligar a cada call site a pasar dos nulls explicitos (punto 24).
     * Antes de esto la insignia no tenia descripcion propia y el texto salia del nombre
     * de la mision, asi que todos los call sites "funcionaban"; ahora que el campo existe,
     * dejarlo en null es una decision y por eso el atajo dice lo que hace en vez de
     * esconderlo.
     */
    public Mision crearMision(
        UUID idAdmin,
        String nombreMision,
        String descripcion,
        String nombreInsignia,
        ReglaConstancia constancia,
        AtributoImpacto atributo,
        Operacion operacion
    ) {
        return crearMision(
            idAdmin,
            nombreMision,
            descripcion,
            nombreInsignia,
            null,
            null,
            constancia,
            atributo,
            operacion
        );
    }

    /**
  * La sobrecarga que recibe la regla ya armada. La de once campos existe para no obligar
  * a quien llama a construir un {@code ReglaConstancia} y una {@code Operacion}: la
  * regla se construye aca a partir de los parametros.
 *
 * <p>Los dos ultimos parametros son la descripcion y la imagen de la <b>insignia</b>, que
 * son distintos de {@code descripcionMision} (punto 24). Aceptan null: el enunciado pide los
 * tres datos de la insignia pero no todos los clientes los mandan, y antes de esto no había
 * por dónde cargarlos siquiera.
 */
    public Mision crearMision(
        UUID idAdmin,
        String nombreMision,
        String descripcionMision,
        String nombreInsignia,
        String descripcionInsignia,
        String urlImagenInsignia,
        ReglaConstancia constancia,
        AtributoImpacto atributo,
        Operacion operacion
    ) {
        Regla regla = new Regla(constancia, atributo, operacion);
        return new Mision(
            nombreMision,
            idAdmin,
            descripcionMision,
            nombreInsignia,
            descripcionInsignia,
            urlImagenInsignia,
            regla
        );
    }

    /**
 * La constancia es opcional: si no viene ninguna de las dos partes, la misión no exige
 * ventana temporal.
 *
 * <p><b>Si viene solo una de las dos es un error, y ahora sí se rechaza (punto 34).</b> El
 * código anterior tenía {@code cantidadTiempo == null || unidadTiempo == null || ...} y
 * devolvía {@code null} en los tres casos, así que una misión a la que le faltaba la mitad
 * de la constancia se guardaba <em>sin exigencia de racha</em>. Por HTTP no se notaba, porque
 * {@code ConstanciaDTO} tiene {@code @NotNull} + {@code @NotBlank} en ambos campos y el
 * bean validation cortaba antes. Pero cualquier llamada interna —un scheduler, un test, el
 * inicializador del seed— creaba una misión sin racha creyendo que sí la tenía, y eso no
 * se detectaba en ningún lado: la misión quedaba configurada con una regla más permisiva
 * que la que el admin quiso.
 *
 * <p>Un medio dato es peor que ningún dato porque no se ve: la misión existe, la regla
 * existe, y lo que falta es la exigencia.
 */
    public ReglaConstancia crearConstancia(Integer cantidadTiempo, String unidadTiempo) {
        boolean sinCantidad = cantidadTiempo == null;
        boolean sinUnidad = unidadTiempo == null || unidadTiempo.isBlank();

        if (sinCantidad && sinUnidad) {
            // Ninguna de las dos: la misión no exige ventana temporal. Es lo válido.
            return null;
        }

        if (sinCantidad || sinUnidad) {
            throw new DatosInvalidosException(
                "La constancia necesita las dos partes: cantidad y unidad de tiempo. "
                    + "Llegó " + (sinCantidad ? "solo la unidad" : "solo la cantidad") + "."
            );
        }

        if (cantidadTiempo <= 0) {
            throw new DatosInvalidosException(
                "La cantidad de la constancia debe ser mayor a cero, llegó: " + cantidadTiempo
            );
        }

        UnidadTiempo unidad = UNIDADES_PERMITIDAS.get(normalizar(unidadTiempo));
        if (unidad == null) {
            throw new DatosInvalidosException(
                "'" + unidadTiempo + "' no es una unidad de tiempo válida. Se aceptan: "
                    + UNIDADES_EN_ESPANOL
            );
        }

        return new ReglaConstancia(cantidadTiempo, unidad);
    }

    /**
     * Traduce el texto del atributo a su valor del enum.
     *
     * <p>Si el texto viene vacío o no corresponde a ningún valor, lanza
     * {@link DatosInvalidosException} con la lista de lo aceptado, que es 400 con un mensaje
     * útil en vez de un 500 por un {@code valueOf} que revienta.
     */
    public AtributoImpacto crearAtributoImpacto(String atributo) {
        if (atributo == null || atributo.isBlank()) {
            throw new DatosInvalidosException(
                "La regla requiere un atributo de impacto. Se aceptan: "
                    + List.of(AtributoImpacto.values())
            );
        }

        try {
            return AtributoImpacto.valueOf(normalizar(atributo));
        } catch (IllegalArgumentException exception) {
            throw new DatosInvalidosException(
                "'" + atributo + "' no es un atributo de impacto válido. Se aceptan: "
                    + List.of(AtributoImpacto.values())
            );
        }
    }

    /**
     * Traduce el texto del tipo de operación a la implementación correspondiente.
     *
     * <p>No hace nada más que delegar en {@link OperacionFactory}, que es quien tiene el
     * {@code switch}. Está acá para que la cadena quede entera dentro de la factory de la
     * misión.
     */
    public Operacion crearOperacion(String tipoOperacion,
                                    Integer progresoObjetivo,
                                    Integer cantidad,
                                    String valor) {
        return operacionFactory.conseguirOperacion(
            tipoOperacion,
            progresoObjetivo,
            cantidad,
            valor
        );
    }

    /**
     * Pasa a mayúsculas y saca los acentos, así el cliente puede mandar "AÑOS",
     * "años", "Anos" o "ANOS" y todas funcionan. Antes había que adivinar si el
     * valor era un nombre de unidad en inglés o en español.
     *
     * <p><b>Es público y estático por el punto 34.</b> {@code RepositorioMisiones.obtenerTodas}
     * tenía su propio {@code valueOf(str.trim().toUpperCase())}, sin normalizar: el mismo
     * endpoint que con el {@code POST} aceptaba "CATEGORÍA" devolvía 400 con el mensaje
     * crudo de Java al pedirlo por query param. Reutilizar esta función es lo que hace que
     * las dos entradas hablen el mismo idioma, y por eso vive acá y no duplicada en el
     * repositorio.
     *
     * @param texto el texto tal cual vino del cliente. No puede ser null.
     */
    public static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                         .replaceAll("\\p{M}", "")
                         .trim()
                         .toUpperCase(Locale.ROOT);
    }
}
