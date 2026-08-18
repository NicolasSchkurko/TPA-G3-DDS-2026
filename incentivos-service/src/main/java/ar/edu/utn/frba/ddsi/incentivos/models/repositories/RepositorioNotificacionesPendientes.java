package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.PerfilNotificacionDTO;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

@Repository
//podriamos hacer un cron donde se pruebe enviar nuevamente estas notificaciones
//a los usuarios; en caso de falla de nuevo, se eliminan y el repo queda vacio
//para recibir nuevas notificaciones pendientes
//quiza el usuario no quiera recibir mas notificaciones(?
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
