package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas;

import java.time.temporal.ChronoUnit;

/**
 * Unidades de tiempo admitidas por las reglas de constancia de las misiones.
 *
 * <p>Los nombres coinciden con los valores que ya estaban persistidos y expuestos por la
 * API, para conservar compatibilidad al reemplazar el enum de la JDK.
 */
public enum UnidadTiempo {
    MINUTES(ChronoUnit.MINUTES),
    HOURS(ChronoUnit.HOURS),
    DAYS(ChronoUnit.DAYS),
    WEEKS(ChronoUnit.WEEKS),
    MONTHS(ChronoUnit.MONTHS),
    YEARS(ChronoUnit.YEARS);

    private final ChronoUnit unidadTemporal;

    UnidadTiempo(ChronoUnit unidadTemporal) {
        this.unidadTemporal = unidadTemporal;
    }

    /** Unidad temporal equivalente para operar con {@code LocalDateTime}. */
    public ChronoUnit comoChronoUnit() {
        return unidadTemporal;
    }
}
