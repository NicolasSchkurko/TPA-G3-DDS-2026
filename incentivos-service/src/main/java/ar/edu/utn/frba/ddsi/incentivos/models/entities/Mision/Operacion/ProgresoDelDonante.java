package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion;

/**
 * El avance de cada donante que una operación puede necesitar para evaluar su regla. Separa
 * el avance individual de la configuración compartida de la misión.
 */
public interface ProgresoDelDonante {

    /**
     * Registra un valor observado por el donante.
     *
     * @param valor el atributo de la donación.
     * @return {@code true} si el valor era nuevo para este donante.
     */
    boolean registrarValorObservado(String valor);

    /** Cuántos valores distintos vio este donante en la misión que está haciendo. */
    int cantidadValoresObservados();
}
