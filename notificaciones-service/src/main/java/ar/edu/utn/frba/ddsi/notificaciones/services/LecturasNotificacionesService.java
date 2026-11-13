package ar.edu.utn.frba.ddsi.notificaciones.services;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.LecturaNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories.RepositorioLecturasNotificaciones;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LecturasNotificacionesService {

    private final RepositorioLecturasNotificaciones repositorio;

    public LecturasNotificacionesService(RepositorioLecturasNotificaciones repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<UUID> listarLeidas(String usuarioId) {
        return repositorio.findNotificacionIdsByUsuarioId(usuarioId);
    }

    @Transactional
    public void marcarLeidas(String usuarioId, Iterable<UUID> notificacionIds) {
        for (UUID notificacionId : notificacionIds) {
            if (!repositorio.existsByUsuarioIdAndNotificacionId(usuarioId, notificacionId)) {
                repositorio.save(new LecturaNotificacion(usuarioId, notificacionId));
            }
        }
    }
}
