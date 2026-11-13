package ar.edu.utn.frba.ddsi.incentivos.models.entities.Notificaciones;

import ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.PerfilNotificacionDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Outbox durable de notificaciones hacia el broker. La fila se guarda cuando la publicación
 * falla y se reintenta con lease y backoff, igual que la outbox de n8n.
 */
@Getter
@Entity
@Table(name = "notificacion_pendiente", indexes = {
        @Index(name = "idx_notificacion_pendiente_reintento",
                columnList = "proximo_intento, lease_hasta")
})
@NoArgsConstructor
public class NotificacionPendiente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String idSolicitud;

    @Column(length = 100)
    private String medioDeContacto;

    @Column(length = 500)
    private String direccionDeContacto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String cuerpoMensaje;

    @Column(nullable = false, length = 500)
    private String asuntoMensaje;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime proximoIntento;

    private LocalDateTime leaseHasta;

    @Column(nullable = false)
    private int intentos;

    @Column(length = 2000)
    private String ultimoError;

    @Column(nullable = false)
    private boolean descartada;

    public NotificacionPendiente(PerfilNotificacionDTO dto, LocalDateTime ahora) {
        this.idSolicitud = dto.getIdSolicitud();
        this.medioDeContacto = dto.getMedioDeContacto();
        this.direccionDeContacto = dto.getDireccionDeContacto();
        this.cuerpoMensaje = dto.getCuerpoMensaje();
        this.asuntoMensaje = dto.getAsuntoMensaje();
        this.fechaCreacion = ahora;
        this.proximoIntento = ahora;
    }

    /** Reconstruye el DTO que se publica en el broker. */
    public PerfilNotificacionDTO comoDto() {
        return new PerfilNotificacionDTO(
                idSolicitud,
                medioDeContacto,
                direccionDeContacto,
                cuerpoMensaje,
                asuntoMensaje
        );
    }

    /** Marca la fila como reclamada y cuenta el intento. */
    public void reclamarHasta(LocalDateTime vencimiento) {
        this.leaseHasta = vencimiento;
        this.intentos++;
    }

    /** Libera el lease y agenda el próximo intento. */
    public void reintentarEn(LocalDateTime proximoIntento, String error) {
        this.proximoIntento = proximoIntento;
        this.leaseHasta = null;
        this.ultimoError = error == null ? null : error.substring(0, Math.min(error.length(), 2000));
    }

    /** Marca la fila como descartada: se deja de reintentar. */
    public void descartar(String error) {
        this.descartada = true;
        this.leaseHasta = null;
        this.ultimoError = error == null ? null : error.substring(0, Math.min(error.length(), 2000));
    }
}
