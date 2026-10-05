package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reacomoda a los donantes de una categoría cuando su secuencia de misiones cambia, y
 * reinicia el avance de los que estaban en una misión cuyo criterio de completado cambió.
 *
 * <p>No depende de {@code DonacionClient} a propósito: antes pedía el contacto de cada
 * donante una vez por iteración del bucle y con la transacción abierta (punto 12), así que
 * recorrer una categoría con 500 donantes eran 500 llamadas HTTP con 500 conexiones del
 * pool retenidas. Ahora los eventos llevan el {@code idUsuario} y el contacto lo resuelve
 * {@code NotificacionClient} en {@code AFTER_COMMIT}, ya fuera de la transacción.
 */
@Slf4j
@Service
public class SincronizacionPerfiles {

    private final RepositorioPerfiles repositorioPerfiles;

    public SincronizacionPerfiles(RepositorioPerfiles repositorioPerfiles) {
        this.repositorioPerfiles = repositorioPerfiles;
    }

    /**
     * Corre a todos los donantes de la categoría a la misión que les toca según la
     * posición nueva.
     *
     * @param posicionesAnteriores dónde estaba cada misión <em>antes</em> del cambio, con
     *                             su id como clave. Es lo que permite saber a quién hay que
     *                             mover: un donante cuya misión cambió de posición puede
     *                             arrancar en otra y perder su avance.
     */
    @Transactional
    public void actualizarMisionesPorCambioDeCategoria(
            Categoria categoria,
            Map<UUID, Integer> posicionesAnteriores
    ) {
        List<Perfil> perfiles = repositorioPerfiles.findAllByCategoriaActual(categoria);

        Map<Integer, Mision> misionesPorPosicion = categoria.getCategoriaMisiones().stream()
                .collect(Collectors.toMap(
                        cm -> cm.getPosicion(),
                        cm -> cm.getMision(),
                        (primera, segunda) -> primera
                ));

        for (Perfil perfil : perfiles) {
            if (perfil.getProgresoMisionActual() == null
                    || perfil.getProgresoMisionActual().getMision() == null) {
                continue;
            }

            Mision misionActual = perfil.getProgresoMisionActual().getMision();
            Integer posicionAnterior = posicionesAnteriores.get(misionActual.getIdMision());
            if (posicionAnterior == null) {
                continue;
            }

            Mision nuevaMision = misionMasCercana(misionesPorPosicion, posicionAnterior);
            // Objects.equals y no un .equals() a secas: si por algún motivo una de las dos
            // misiones no estuviera persistida, getIdMision() devuelve null y el
            // .equals() reventaba con NullPointerException en medio de la transacción,
            // con lo cual la categoría se quedaba a medio reacomodar. Con Objects.equals
            // dos ids null dan true, que para este caso es justo lo que se quiere: si las
            // dos son la misma fila, el donante no se toca.
            if (nuevaMision == misionActual
                    || (nuevaMision != null
                    && Objects.equals(nuevaMision.getIdMision(), misionActual.getIdMision()))) {
                continue;
            }

            // Si le corresponde otra posición, se lo mueve.
            if (nuevaMision == null) {
                // Es el único caso en que quedarse sin misión es correcto: el admin dejó
                // la categoría sin ninguna misión. Antes esto pasaba en silencio y el
                // donante quedaba bloqueado sin que nadie supiera por qué, así que
                // ahora queda en el log (punto 30).
                log.warn("El donante {} queda sin misión: la categoría {} se quedó sin misiones",
                        perfil.getIdUsuario(), categoria.getNombre());
            }

            perfil.cambiarMision(nuevaMision, misionActual);
        }

        repositorioPerfiles.saveAll(perfiles);
    }

    /**
     * La misión que le corresponde a un donante que estaba en la posición dada, cuando esa
     * posición ya no existe (punto 30).
     *
     * <p><b>Por qué hace falta.</b> El admin edita la categoría con {@code PUT
     * /api/categorias/admin/{id}} y puede dejar missions menos. Si la categoría tenía
     * {@code [A(1), B(2), C(3)]} con 40 donantes en {@code C} y el admin manda
     * {@code "misiones": ["A", "B"]}, la posición 3 desaparece. El código de antes pedía
     * {@code misionesPorPosicion.get(3)}, recibía {@code null} y entraba al {@code return}
     * temprano de {@code Perfil.cambiarMision}, que solo hace
     * {@code progresoMisionActual = null}. Ese donante quedaba <b>bloqueado para
     * siempre</b>: sin misión no progresa, no completa nada, no recibe insignias y no
     * aparece en el ranking. Y no se avisaba: ni evento de cambio de misión ni
     * notificación, así que tampoco había forma de enterarse desde afuera. Solo se
     * descubría preguntándole a un donante por qué no le avanzaba nada.
     *
     * <p><b>Qué hace en su lugar.</b> Le busca la misión más cercana por debajo, o sea la
     * última que le corresponde de las que quedaron: el donante retrocede lo mínimo, que es
     * lo más justo con el avance que ya hizo. Si le sacaron la primera y no quedó ninguna
     * por debajo, arranca por la que ahora sea la primera. Y si la categoría se quedó sin
     * misiones, ahí sí no hay nada que ofrecerle y se devuelve {@code null}: ese es el
     * único caso en que quedar sin misión es correcto.
     *
     * <p><b>El precio.</b> Al crear un {@code ProgresoMision} nuevo, el donante pierde el
     * avance que tenía en la misión que se le sacó. Es la consecuencia de quitarle una
     * misión al programa a mitad de camino, y es preferible a dejarlo congelado: perder
     * progreso es una molestia, quedarse trabado no tiene arreglo.
     *
     * @param posiciones     las misiones de la categoría con su posición, ya cargadas
     * @param posicionAnterior la posición que tenía la misión del donante
     * @return la misión que le toca ahora, o {@code null} solo si la categoría quedó vacía
     */
    private Mision misionMasCercana(Map<Integer, Mision> misionesPorPosicion, int posicionAnterior) {
        // Caso normal: la posición sigue existiendo y le toca la misma de siempre.
        if (misionesPorPosicion.containsKey(posicionAnterior)) {
            return misionesPorPosicion.get(posicionAnterior);
        }

        // La última de las que quedan antes de donde estaba: retrocede lo mínimo.
        return misionesPorPosicion.keySet().stream()
                .filter(posicion -> posicion < posicionAnterior)
                .max(Integer::compareTo)
                .map(misionesPorPosicion::get)
                // Le sacaron la primera y no quedó nada por debajo: arranca por la primera.
                .orElseGet(() -> misionesPorPosicion.entrySet().stream()
                        .min(Map.Entry.comparingByKey())
                        .map(Map.Entry::getValue)
                        .orElse(null));
    }

    /**
     * Reinicia el avance de todos los que están en una misión, porque su criterio de
     * completado cambió (punto 15).
     *
     * <p>La lógica vive acá y no en un {@code default} del repositorio, que es lo que había
     * antes: una interfaz de repositorio debería decir cómo consultar y guardar, no cuándo
     * hay que borrar el avance de un donante.
     *
     * <p>Delega en el agregado ({@code ProgresoMision.reiniciarProgreso}) en vez de tocar
     * campos sueltos, y eso importa: hay que limpiar también los valores observados, no solo
     * el contador. Si solo se pone el contador en 0, al subir de 3 a 5 categorías distintas
     * el donante conserva las 3 que ya había visto y solo necesita 2 nuevas en lugar de 5.
     */
    @Transactional
    public void reiniciarProgresoDeMision(UUID idMision) {
        List<Perfil> perfiles = repositorioPerfiles.findAllByMisionActual(idMision);

        for (Perfil perfil : perfiles) {
            if (perfil.getProgresoMisionActual() != null) {
                perfil.getProgresoMisionActual().reiniciarProgreso();
            }
        }

        repositorioPerfiles.saveAll(perfiles);
    }
}
