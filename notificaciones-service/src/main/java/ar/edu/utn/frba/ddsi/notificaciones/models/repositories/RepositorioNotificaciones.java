package ar.edu.utn.frba.ddsi.notificaciones.models.repositories;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * Repositorio en memoria de notificaciones.
 * Mantiene una lista por cada EstadoNotificacion y se inicializa desde el enum para soportar nuevos estados.
 */
@Repository
public interface RepositorioNotificaciones extends JpaRepository<Notificacion, UUID> {

    // Spring Data JPA implementa automáticamente esta consulta SQL:
    // SELECT * FROM notificaciones WHERE estado = ?
    List<Notificacion> findByEstado(EstadoNotificacion estado);

    // Métodos como save(), findById(UUID id), findAll() ya vienen incluidos en JpaRepository
}

/*
@Repository("repositorio")
public interface RepositorioNotificaciones extends JpaRepository<Notificacion, String> {
    private final Map<EstadoNotificacion, List<Notificacion>> notificacionesPorEstado;
    private final List<Notificacion> notificaciones = new ArrayList<>();

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

    public Optional<Notificacion> findById(UUID id) {
        return notificaciones.stream()
                .filter(n -> n.getId().equals(id))
                .findFirst();
    }

    public List<Notificacion> buscarTodas() {
        return this.notificacionesPorEstado.values()
                .stream()
                .flatMap(List::stream)
                .toList();
    }
}
*/
