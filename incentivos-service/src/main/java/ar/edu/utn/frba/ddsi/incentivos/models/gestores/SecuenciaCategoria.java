package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mantiene la secuencia de posiciones de las categorías sin huecos ni repeticiones,
 * desplazando en bloque las que quedan de la posición movida en adelante.
 */
@Component
public class SecuenciaCategoria {

    /**
     * Corrige la secuencia cuando una categoría cambia de posición. Una posición fuera de
     * rango es un error, no un "no hacer nada".
     *
     * @param posicionMaxima la posición más alta realmente ocupada.
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
     * Rechaza una posición fuera de rango. El tope es {@code max + 1} en el alta y {@code max}
     * en la edición.
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
     * Deja libre la posición de una categoría nueva, bajando las que están de ahí para
     * arriba. El tope es {@code max + 1} porque la categoría nueva hace una más.
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

        // Se resuelve a primitivo: con la base vacía posicionMaxima viene en null.
        if (posicionNueva > posicionMaximaOpcero(posicionMaxima)) {
            // Va al final: no hay a quién correr.
            return;
        }

        repo.desplazarHaciaAbajoDesde(posicionNueva);
    }

    /** Cierra el hueco que deja la categoría borrada, subiendo las que estaban después. */
    public void desplazarParaEliminar(RepositorioCategorias repo, Integer posicionLiberada) {
        if (posicionLiberada != null) {
            repo.desplazarHaciaArribaDesde(posicionLiberada + 1);
        }
    }

    /** La posición más alta ocupada, o {@code null} si no hay ninguna. */
    public Integer posicionMaxima(RepositorioCategorias repo) {
        List<Integer> posiciones = repo.listarPosiciones();
        return posiciones.isEmpty() ? null : posiciones.get(posiciones.size() - 1);
    }
}
