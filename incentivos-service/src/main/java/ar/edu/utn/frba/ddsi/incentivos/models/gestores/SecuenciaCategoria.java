package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mantiene la secuencia de posiciones de las categorías sin huecos ni repeticiones.
 *
 * <p>El mecanismo es desplazar en bloque: si entra una categoría en la posición 3, todas
 * las que estaban de la 3 en adelante bajan un lugar para dejarle el hueco.
 *
 * <p>Sobre la unicidad (punto 19): el invariante se garantiza <b>en la aplicación</b>,
 * comprobando que la posición este libre antes de guardar, y no con un
 * {@code unique = true} en la base. La razón es que los {@code UPDATE} de este gestor
 * mueven varias filas de a uno en una sola sentencia, y eso es incompatible con que la
 * base verifique la unicidad en el momento: al pasar la fila de la posición 3 a la 4
 * pisaria a la que todavia sigue en la 4 y la base lo rechazaria, dejando la secuencia a
 * medias. Garantizarlo con un trigger o con UPDATE fila por fila exigiria reescribir el
 * gestor; para el tamaño de este servicio, el chequeo en la capa de aplicación con un
 * mensaje claro al cliente cumple.
 */
@Component
public class SecuenciaCategoria {

    /**
     * Corrijo la secuencia cuando una categoría cambia de posición.
 *
     * <p><b>Una posición fuera de rango es un error, no un "no hacer nada" (punto 31).</b>
     * Antes el método salía en silencio con un {@code return} y el caller igual escribía la
     * posición pedida, así que la secuencia quedaba con huecos y el invariante "sin huecos"
     * que esta misma clase declara quedaba roto. El caso peor era {@code posicionSecuencia:
     * 0}: la categoría quedaba en 0, y como la base del programa se elige con la posición
     * más baja, <b>todos los donantes nuevos pasaban a arrancar en esa categoría</b> en vez
     * de en la base.
     *
     * <p>Ahora se lanza {@link IllegalArgumentException}, que el handler traduce a 400: el
     * admin recibe un error que dice qué posición pidió y cuál era la válida, en vez de un
     * 200 con una secuencia rota que nadie se entera hasta que un donante no progresa.
     *
     * @param posicionMaxima la posición más alta realmente ocupada. Antes se pasaba
     *                       {@code repo.count()}, que da por hecho que las posiciones
     *                       son 1..N; con un hueco en la secuencia ese límite era
     *                       incorrecto y el desplazamiento movía categorías que no
     *                       correspondía.
     * @throws IllegalArgumentException si la posición está fuera de {@code [1, max]}
     */
    public void desplazarParaActualizar(RepositorioCategorias repo,
                                        Integer posicionAnterior,
                                        Integer posicionNueva,
                                        Integer posicionMaxima) {
        if (posicionNueva == null || posicionAnterior == null || posicionNueva.equals(posicionAnterior)) {
            return;
        }

        verificarPosicionEnRango(posicionNueva, posicionMaxima, false);

        if (posicionNueva < posicionAnterior) {
            // Subió: hay que bajar a las que quedaron entre las dos posiciones.
            repo.desplazarHaciaAbajo(posicionNueva, posicionAnterior - 1);
        } else {
            // Bajó: hay que subir a las que quedaron entre las dos posiciones.
            repo.desplazarHaciaArriba(posicionAnterior + 1, posicionNueva);
        }
    }

    /**
     * Rechaza una posición fuera de rango, diciendo cuál era la válida.
     *
     * <p><b>El rango es distinto en el alta y en la edición, y no es un descuido.</b>
     *
     * <ul>
     *   <li>En el <b>alta</b> el rango es {@code [1, max + 1]}: la categoría nueva va a ser
     *       la sexta de cinco, así que la última posición posible es la 6.</li>
     *   <li>En la <b>edición</b> el rango es {@code [1, max]}: el número de categorías no
     *       cambia, así que la 6 de cinco no existe. Admitirla dejaría un hueco —la categoría
     *       que estaba en la 2 se va a la 6 y las de la 3 a la 5 no corren, con lo que la
     *       secuencia queda {@code 1,_,3,4,5,6}—, que es exactamente el invariante roto que
     *       este punto viene a cerrar.</li>
     * </ul>
     *
     * @param posicionMaxima el límite superior real, o {@code null} si no hay categorías con
     *                       posición
     * @param esAlta         si la posición es para una categoría nueva
     */
    private void verificarPosicionEnRango(Integer posicion, Integer posicionMaxima, boolean esAlta) {
        int maxima = posicionMaximaOpcero(posicionMaxima);
        int tope = esAlta ? maxima + 1 : maxima;

        if (posicion < 1) {
            throw new IllegalArgumentException(
                    "La posición en la secuencia tiene que ser 1 o más, no " + posicion + ".");
        }

        if (posicion > tope) {
            throw new IllegalArgumentException(
                    "La posición " + posicion + " está fuera de rango: el programa tiene "
                            + maxima + " categoría"
                            + (maxima == 1 ? "" : "s")
                            + ", así que la posición válida va de 1 a " + tope + ".");
        }
    }

    /** El máximo de la secuencia como primitivo, con 0 cuando no hay ninguna categoría. */
    private int posicionMaximaOpcero(Integer posicionMaxima) {
        return posicionMaxima == null ? 0 : posicionMaxima;
    }

    /**
     * Deja libre la posición de una categoría nueva, bajando una posición todas las que están
     * de ahí para arriba.
     *
     * <p>Existe con el mismo criterio de rechazo que {@link #desplazarParaActualizar}
     * (punto 31), y por el mismo motivo: el bug era idéntico en el alta, porque
     * {@code desplazarHaciaAbajoDesde(10)} no mueve nada y la categoría se guardaba igual en
     * la 10. Solo cambia el tope del rango, que acá es {@code max + 1} porque la categoría
     * nueva hace una más.
     *
     * @throws IllegalArgumentException si la posición está fuera de {@code [1, max + 1]}
     */
    public void desplazarParaCrear(RepositorioCategorias repo,
                                    Integer posicionNueva,
                                    Integer posicionMaxima) {
        if (posicionNueva == null) {
            return;
        }

        verificarPosicionEnRango(posicionNueva, posicionMaxima, true);

        // El máximo va resuelto a primitivo y no el Integer de entrada: con la base vacía
        // posicionMaxima viene en null y compararlo sin desempaquetar revienta con un NPE
        // justo en la creación de la primera categoría.
        if (posicionNueva > posicionMaximaOpcero(posicionMaxima)) {
            // Va al final: no hay a quién correr.
            return;
        }

        repo.desplazarHaciaAbajoDesde(posicionNueva);
    }

    /**
     * Cierra el hueco que deja la categoría borrada, subiendo una posición todas las que
     * estaban después.
     */
    public void desplazarParaEliminar(RepositorioCategorias repo, Integer posicionLiberada) {
        if (posicionLiberada != null) {
            repo.desplazarHaciaArribaDesde(posicionLiberada + 1);
        }
    }

    /**
     * La posición más alta ocupada, o {@code null} si no hay ninguna categoría con
     * posición. Es el límite superior real de la secuencia.
     */
    public Integer posicionMaxima(RepositorioCategorias repo) {
        List<Integer> posiciones = repo.listarPosiciones();
        return posiciones.isEmpty() ? null : posiciones.get(posiciones.size() - 1);
    }
}
