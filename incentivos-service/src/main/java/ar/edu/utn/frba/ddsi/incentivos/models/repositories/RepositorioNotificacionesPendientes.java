package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.PerfilNotificacionDTO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Cola en memoria de notificaciones que no se pudieron entregar.
 *
 * <p>Es solo un buffer transitorio: se pierde al reiniciar el servicio y no se
 * comparte entre replicas, asi que una notificacion fallida nunca se reintenta.
 * El reemplazo por una tabla de outbox con scheduler de reintento esta
 * trackeado en {@code PENDIENTES.md} (punto 3).
 */
@Repository
public class RepositorioNotificacionesPendientes {
    private final List<PerfilNotificacionDTO> pendientes;

    public RepositorioNotificacionesPendientes() {
        this.pendientes = new ArrayList<>();
    }

    public void guardar(PerfilNotificacionDTO pendiente) {
        if (pendiente != null && !pendientes.contains(pendiente)) {
            pendientes.add(pendiente);
        }
    }

    public void eliminar(PerfilNotificacionDTO pendiente) {
        pendientes.remove(pendiente);
    }

    public List<PerfilNotificacionDTO> listarTodas() {
        return List.copyOf(pendientes);
    }
}
