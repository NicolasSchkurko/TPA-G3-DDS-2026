package ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.LecturaNotificacion;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RepositorioLecturasNotificaciones extends JpaRepository<LecturaNotificacion, UUID> {
    @Query("select lectura.notificacionId from LecturaNotificacion lectura where lectura.usuarioId = :usuarioId")
    List<UUID> findNotificacionIdsByUsuarioId(@Param("usuarioId") String usuarioId);

    boolean existsByUsuarioIdAndNotificacionId(String usuarioId, UUID notificacionId);
}
