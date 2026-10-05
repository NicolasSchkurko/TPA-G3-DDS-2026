package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import org.springframework.stereotype.Component;

import java.util.List;

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

    public SecuenciaCategoria() {
    }

    /**
     * Deja libre la posición pedida, bajando una posición todas las que están de ahí para
     * arriba. Se llama antes de guardar la categoría nueva.
     */
    public void desplazarParaCrear(RepositorioCategorias repo, Integer posicionNueva) {
        if (posicionNueva != null) {
            repo.desplazarHaciaAbajoDesde(posicionNueva);
        }
    }

    /**
     * Corrijo la secuencia cuando una categoría cambia de posición.
     *
     * @param posicionMaxima la posición más alta realmente ocupada. Antes se pasaba
     *                       {@code repo.count()}, que da por hecho que las posiciones
     *                       son 1..N; con un hueco en la secuencia ese límite era
     *                       incorrecto y el desplazamiento movía categorías que no
     *                       correspondía.
     */
    public void desplazarParaActualizar(RepositorioCategorias repo,
                                        Integer posicionAnterior,
                                        Integer posicionNueva,
                                        Integer posicionMaxima) {
        if (posicionNueva == null || posicionAnterior == null || posicionNueva.equals(posicionAnterior)) {
            return;
        }

        if (posicionMaxima == null || posicionNueva < 1 || posicionNueva > posicionMaxima) {
            return;
        }

        if (posicionNueva < posicionAnterior) {
            // Subió: hay que bajar a las que quedaron entre las dos posiciones.
            repo.desplazarHaciaAbajo(posicionNueva, posicionAnterior - 1);
        } else {
            // Bajó: hay que subir a las que quedaron entre las dos posiciones.
            repo.desplazarHaciaArriba(posicionAnterior + 1, posicionNueva);
        }
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