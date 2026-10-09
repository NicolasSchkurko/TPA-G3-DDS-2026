package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.PerfilNotificacionDTO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Cola en memoria de notificaciones que no se pudieron entregar. Es un buffer transitorio:
 * se pierde al reiniciar y no se comparte entre réplicas.
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
