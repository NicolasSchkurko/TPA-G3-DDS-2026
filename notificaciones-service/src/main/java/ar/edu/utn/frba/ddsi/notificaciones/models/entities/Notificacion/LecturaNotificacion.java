package ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "notificacion_lecturas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notificacion_lectura_usuario_notificacion",
                columnNames = {"usuario_id", "notificacion_id"}))
public class LecturaNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false, length = 255)
    private String usuarioId;

    @Column(name = "notificacion_id", nullable = false)
    private UUID notificacionId;

    protected LecturaNotificacion() {
    }

    public LecturaNotificacion(String usuarioId, UUID notificacionId) {
        this.usuarioId = usuarioId;
        this.notificacionId = notificacionId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public UUID getNotificacionId() {
        return notificacionId;
    }
}
