package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion;

/**
 * El avance del donante que una operación puede necesitar para evaluar su regla.
 *
 * <p>Existe para no volver a mezclar dos cosas que son de distinta naturaleza:
 *
 * <ul>
 *   <li>La <b>configuración</b> de una misión (qué tiene que hacer el donante: "6
 *       donaciones de 3 categorías distintas"). Es una sola fila compartida por todos
 *       los que hacen esa misión.</li>
 *   <li>El <b>avance</b> de cada donante (qué valores vio él). Es suyo y de nadie más.</li>
 * </ul>
 *
 * <p>Sin esta separación, las reglas que necesitan recordar qué values vio el donante
 * guardaban esa lista dentro de la entidad de la misión, y quedaba compartida entre
 * todos: al tercer donante la misión ya figuraba completa para los tres, y se
 * otorgaba una insignia que dos de ellos no se habían ganado.
 */
public interface ProgresoDelDonante {

    /**
     * Registra un valor observado por el donante, si todavía no lo había visto.
     *
     * @param valor el atributo de la donación, ya sea un texto legible.
     * @return {@code true} si el valor era nuevo para este donante.
     */
    boolean registrarValorObservado(String valor);

    /** Cuántos valores distintos vio este donante en la misión que está haciendo. */
    int cantidadValoresObservados();
}
