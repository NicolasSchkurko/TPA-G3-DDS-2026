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
     * Traduce el nombre de unidad. Se aceptan los nombres en español y los identificadores
     * en inglés que ya se persistían.
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

    /** La sobrecarga para quien solo tiene el nombre de la insignia. */
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

    /** La sobrecarga que recibe la regla ya armada y los datos propios de la insignia. */
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

    /** Construye la constancia: sin ninguna parte no exige ventana; con una sola es un error. */
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

    /** Traduce el texto del atributo a su valor del enum. */
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

    /** Traduce el texto del tipo de operación a la implementación correspondiente. */
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
     * Pasa a mayúsculas y saca los acentos, así "AÑOS", "años" y "ANOS" son equivalentes.
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
