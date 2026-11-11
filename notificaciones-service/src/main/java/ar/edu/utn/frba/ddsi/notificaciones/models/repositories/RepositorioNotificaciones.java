package ar.edu.utn.frba.ddsi.notificaciones.models.repositories;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioNotificaciones extends JpaRepository<Notificacion, UUID> {
    Optional<Notificacion> findByIdSolicitud(String idSolicitud);

    Page<Notificacion> findByDireccionDeContactoIgnoreCase(String direccionDeContacto, Pageable pageable);

    Page<Notificacion> findByDireccionDeContactoIgnoreCaseAndEstado(String direccionDeContacto,
                                                                    EstadoNotificacion estado,
                                                                    Pageable pageable);
}
