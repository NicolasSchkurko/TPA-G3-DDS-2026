package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import java.text.Normalizer;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Arma una {@link Mision} completa a partir de los campos sueltos del admin, validando el
 * texto libre que viene del JSON.
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
     * Traduce el nombre de la unidad a su {@link ChronoUnit}. Se aceptan nombres en español
     * e inglés, pero solo de minutos a años.
     */
    private static final Map<String, ChronoUnit> UNIDADES_PERMITIDAS = unidadesPermitidas();

    private static Map<String, ChronoUnit> unidadesPermitidas() {
        Map<String, ChronoUnit> unidades = new LinkedHashMap<>();

        unidades.put("MINUTOS", ChronoUnit.MINUTES);
        unidades.put("HORAS", ChronoUnit.HOURS);
        unidades.put("DIAS", ChronoUnit.DAYS);
        unidades.put("SEMANAS", ChronoUnit.WEEKS);
        unidades.put("MESES", ChronoUnit.MONTHS);
        unidades.put("ANOS", ChronoUnit.YEARS);

        // Los nombres en inglés se aceptan porque así se guardaron en la base.
        unidades.put("MINUTES", ChronoUnit.MINUTES);
        unidades.put("HOURS", ChronoUnit.HOURS);
        unidades.put("DAYS", ChronoUnit.DAYS);
        unidades.put("WEEKS", ChronoUnit.WEEKS);
        unidades.put("MONTHS", ChronoUnit.MONTHS);
        unidades.put("YEARS", ChronoUnit.YEARS);

        return Map.copyOf(unidades);
    }

    /**
     * Arma la misión completa desde los campos sueltos del DTO del admin. La constancia es
     * opcional, pero si viene solo una de sus partes es un error.
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

    /** Sobrecarga para quien solo tiene el nombre de la insignia. */
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

    /** Sobrecarga que recibe la regla ya armada en vez de los parámetros sueltos. */
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
     * Arma la constancia. Es opcional si no viene ninguna de las dos partes; si viene solo
     * una, es un error y no una ausencia.
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

        ChronoUnit unidad = UNIDADES_PERMITIDAS.get(normalizar(unidadTiempo));
        if (unidad == null) {
            throw new DatosInvalidosException(
                "'" + unidadTiempo + "' no es una unidad de tiempo válida. Se aceptan: "
                    + UNIDADES_EN_ESPANOL
            );
        }

        return new ReglaConstancia(cantidadTiempo, unidad);
    }

    /**
     * Traduce el texto del atributo a su valor del enum, o lanza
     * {@link DatosInvalidosException} con la lista de lo aceptado.
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

    /** Delega en {@link OperacionFactory} para que la cadena quede dentro de la factory. */
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
     * Pasa a mayúsculas y saca los acentos para aceptar "AÑOS", "años" o "ANOS". Es público
     * para reutilizarlo desde el repositorio y que query params y body hablen igual.
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
