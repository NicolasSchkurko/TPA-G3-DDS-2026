package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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
     * Traduce el nombre de la unidad a su {@link ChronoUnit}. Se aceptan los nombres
     * en español y los de {@link ChronoUnit} en inglés.
     *
     * <p>El comentario de {@link ReglaConstancia} ya declaraba que la ventana temporal
     * iba de minutos a años, pero el código hacía {@code ChronoUnit.valueOf} sobre
     * cualquier texto y aceptaba {@code FOREVER} o {@code NANOS}, que no significan
     * nada para una racha de donaciones.
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

        // Los nombres en inglés quedan aceptados porque así se guardaron en la base:
        // la columna es un enum con EnumType.STRING.
        unidades.put("MINUTES", ChronoUnit.MINUTES);
        unidades.put("HOURS", ChronoUnit.HOURS);
        unidades.put("DAYS", ChronoUnit.DAYS);
        unidades.put("WEEKS", ChronoUnit.WEEKS);
        unidades.put("MONTHS", ChronoUnit.MONTHS);
        unidades.put("YEARS", ChronoUnit.YEARS);

        return Map.copyOf(unidades);
    }

    public Mision crearMision(
        UUID idAdmin,
        String nombreMision,
        String descripcion,
        String nombreInsignia,
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
            constancia,
            atributoImpacto,
            operacion
        );
    }

    /**
     * La constancia es opcional: si no viene cantidad o unidad, la misión no exige
     * ventana temporal. Si viene solo una de las dos, es un error y no una ausencia de
     * constancia, así que se rechaza en vez de ignorarse.
     */
    public ReglaConstancia crearConstancia(Integer cantidadTiempo, String unidadTiempo) {
        if (cantidadTiempo == null || unidadTiempo == null || unidadTiempo.isBlank()) {
            return null;
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

    public Mision crearMision(
        UUID idAdmin,
        String nombreMision,
        String descripcion,
        String nombreInsignia,
        ReglaConstancia constancia,
        AtributoImpacto atributo,
        Operacion operacion
    ) {
        Regla regla = new Regla(constancia, atributo, operacion);
        return new Mision(nombreMision, idAdmin, descripcion, nombreInsignia, regla);
    }

    /**
     * Pasa a mayúsculas y saca los acentos, así el cliente puede mandar "AÑOS",
     * "años", "Anos" o "ANOS" y todas funcionan. Antes había que adivinar si el
     * valor era un nombre de {@link ChronoUnit} en inglés o en español.
     */
    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                         .replaceAll("\\p{M}", "")
                         .trim()
                         .toUpperCase(Locale.ROOT);
    }
}