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
 * reinicia el avance de los que estaban en una misión cuyo criterio cambió.
 */
@Slf4j
@Service
public class SincronizacionPerfiles {

    private final RepositorioPerfiles repositorioPerfiles;

    public SincronizacionPerfiles(RepositorioPerfiles repositorioPerfiles) {
        this.repositorioPerfiles = repositorioPerfiles;
    }

    /**
     * Corre a todos los donantes de la categoría a la misión que les toca según la posición
     * nueva.
     *
     * @param posicionesAnteriores dónde estaba cada misión antes del cambio, con su id como
     *                             clave.
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
            // Objects.equals tolera ids null: si ambas misiones son la misma fila, no se toca.
            if (nuevaMision == misionActual
                    || (nuevaMision != null
                    && Objects.equals(nuevaMision.getIdMision(), misionActual.getIdMision()))) {
                continue;
            }

            // Si le corresponde otra posición, se lo mueve.
            if (nuevaMision == null) {
                // Único caso correcto de quedarse sin misión: la categoría quedó vacía.
                log.warn("El donante {} queda sin misión: la categoría {} se quedó sin misiones",
                        perfil.getIdUsuario(), categoria.getNombre());
            }

            perfil.cambiarMision(nuevaMision, misionActual);
        }

        repositorioPerfiles.saveAll(perfiles);
    }

    /**
     * La misión que le corresponde a un donante que estaba en una posición que ya no existe:
     * busca la más cercana por debajo, y si no hay, la primera. {@code null} solo si la
     * categoría quedó vacía. Al crear un {@code ProgresoMision} nuevo pierde el avance de la
     * misión que se le sacó.
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
     * Reinicia el avance de todos los que están en una misión, porque su criterio cambió.
     * Delega en {@code ProgresoMision.reiniciarProgreso} para limpiar también los valores
     * observados, no solo el contador.
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
