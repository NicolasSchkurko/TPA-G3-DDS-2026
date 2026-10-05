package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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

            Mision nuevaMision = misionesPorPosicion.get(posicionAnterior);
            if (nuevaMision == misionActual
                    || (nuevaMision != null
                    && nuevaMision.getIdMision().equals(misionActual.getIdMision()))) {
                continue;
            }

            // La posición no cambió para este donante: sigue en la misma misión.
            perfil.cambiarMision(nuevaMision, misionActual);
        }

        repositorioPerfiles.saveAll(perfiles);
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
