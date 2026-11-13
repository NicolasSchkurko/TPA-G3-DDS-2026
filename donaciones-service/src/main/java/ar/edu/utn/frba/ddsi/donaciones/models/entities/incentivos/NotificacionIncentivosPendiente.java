package ar.edu.utn.frba.ddsi.donaciones.models.entities.incentivos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Outbox durable de notificaciones hacia incentivos-service (Punto 76).
 *
 * <p>La fila se guarda en la misma transacción que la entrega o la actualización del
 * donante, así que no se pierde si el servicio de incentivos está caído. El gestor la
 * reclama con un lease, la envía y la borra al confirmar; si falla, la reprograma con
 * backoff. El id de la donación viaja como clave de idempotencia del lado de incentivos.
 */
@Entity
@Table(name = "notificacion_incentivos_pendiente", indexes = {
    @Index(name = "idx_notificacion_incentivos_reintento",
        columnList = "proximo_intento, lease_hasta")
})
@Getter
@NoArgsConstructor
public class NotificacionIncentivosPendiente {

    @Id
    private UUID id = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoNotificacionIncentivos tipo;

    @Column(nullable = false)
    private UUID idUsuario;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime proximoIntento;

    private LocalDateTime leaseHasta;

    @Column(nullable = false)
    private int intentos;

    @Column(length = 2000)
    private String ultimoError;

    public NotificacionIncentivosPendiente(TipoNotificacionIncentivos tipo,
                                           UUID idUsuario,
                                           String payloadJson,
                                           LocalDateTime ahora) {
        this.tipo = tipo;
        this.idUsuario = idUsuario;
        this.payloadJson = payloadJson;
        this.fechaCreacion = ahora;
        this.proximoIntento = ahora;
    }

    public void reclamarHasta(LocalDateTime vencimiento) {
        this.leaseHasta = vencimiento;
        this.intentos++;
    }

    public void reintentarEn(LocalDateTime proximoIntento, String error) {
        this.proximoIntento = proximoIntento;
        this.leaseHasta = null;
        this.ultimoError = error == null ? null : error.substring(0, Math.min(error.length(), 2000));
    }
}
