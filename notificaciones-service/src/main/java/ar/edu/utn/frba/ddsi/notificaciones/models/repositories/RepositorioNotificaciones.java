package ar.edu.utn.frba.ddsi.notificaciones.models.repositories;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio en memoria de notificaciones.
 * Mantiene una lista por cada EstadoNotificacion y se inicializa desde el enum para soportar nuevos estados.
 */
@Repository
public class RepositorioNotificaciones {
    private final Map<EstadoNotificacion, List<Notificacion>> notificacionesPorEstado;

    public RepositorioNotificaciones() {
        this.notificacionesPorEstado = new EnumMap<>(EstadoNotificacion.class);

        for (EstadoNotificacion estado : EstadoNotificacion.values()) {
            this.notificacionesPorEstado.put(estado, new ArrayList<>());
        }
    }

    public void guardar(Notificacion notificacion) {
        List<Notificacion> notificaciones = this.notificacionesPorEstado.computeIfAbsent(
                notificacion.getEstado(),
                estado -> new ArrayList<>()
        );

        if (!notificaciones.contains(notificacion)) {
            notificaciones.add(notificacion);
        }
    }

    public List<Notificacion> buscarPorEstado(EstadoNotificacion estado) {
        return Collections.unmodifiableList(
                this.notificacionesPorEstado.computeIfAbsent(estado, estadoNotificacion -> new ArrayList<>())
        );
    }

    public List<Notificacion> buscarTodas() {
        return this.notificacionesPorEstado.values()
                .stream()
                .flatMap(List::stream)
                .toList();
    }
}
