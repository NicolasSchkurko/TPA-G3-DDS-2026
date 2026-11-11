package ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "notificaciones", indexes = {
        @Index(name = "idx_notificaciones_reintento", columnList = "estado, fecha_creacion, intentos"),
        @Index(name = "idx_notificaciones_fecha", columnList = "fecha_creacion")})
@NoArgsConstructor
public class Notificacion implements Persistable<UUID> {

    @Id
    private UUID id = UUID.randomUUID();

    /**
     * Spring Data decide persist vs merge con esto: el id ya viene asignado, así que sin el flag
     * creería que la fila existe y haría un SELECT antes de cada INSERT.
     */
    @Transient
    private boolean isNew = true;

    @Column(name = "id_solicitud", unique = true)
    private String idSolicitud;

    /** Cascade ALL para persistir el Mensaje junto con la notificación. */
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id_mensaje", referencedColumnName = "id_mensaje")
    private Mensaje mensaje;

    @Column(name = "direccion_contacto", nullable = false)
    private String direccionDeContacto;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    /** Nullable: nace PENDIENTE y no tiene fecha de envío hasta que se envía. */
    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    /** Momento del reclamo ENVIANDO, para detectar envíos colgados. Null fuera de ese estado. */
    @Column(name = "fecha_intento")
    private LocalDateTime fechaIntento;

    /** Token del claim ENVIANDO: el cierre solo aplica si sigue siendo el mismo envío. */
    @Column(name = "claim_id")
    private UUID claimId;

    /** Intentos de reintento automático. Las filas viejas pueden tener NULL; las nuevas nacen en 0. */
    @Column(name = "intentos")
    private Integer intentos = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoNotificacion estado;

    @Column(name = "tipo_medio_contacto", nullable = false)
    private String tipoMedioDeContacto;

    public Notificacion(String direccionDeContacto, String tipoMedioDeContacto, Mensaje mensaje) {
        this(null, direccionDeContacto, tipoMedioDeContacto, mensaje);
    }

    public Notificacion(String idSolicitud, String direccionDeContacto, String tipoMedioDeContacto, Mensaje mensaje) {
        this.idSolicitud = idSolicitud;
        this.fechaCreacion = LocalDateTime.now();
        this.estado = EstadoNotificacion.PENDIENTE;
        this.direccionDeContacto = direccionDeContacto;
        this.tipoMedioDeContacto = tipoMedioDeContacto;
        this.mensaje = mensaje;
    }

    /** Arma una solicitud nueva con su mensaje; el repositorio se encarga de persistirla. */
    public static Notificacion crear(String idSolicitud,
                                     String tipoMedioDeContacto,
                                     String direccionDeContacto,
                                     String asunto,
                                     String cuerpo,
                                     String urlMapa) {
        return new Notificacion(idSolicitud, direccionDeContacto, tipoMedioDeContacto,
                new Mensaje(asunto, cuerpo, urlMapa));
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    private void marcarComoExistente() {
        this.isNew = false;
    }

    /** Reclama la notificación antes de salir a n8n; el timestamp permite recuperar colgadas. */
    public void marcarEnviando(LocalDateTime momento) {
        this.estado = EstadoNotificacion.ENVIANDO;
        this.fechaIntento = momento;
        this.claimId = UUID.randomUUID();
    }

    public void marcarEnviada() {
        this.estado = EstadoNotificacion.ENVIADA;
        this.fechaEnvio = LocalDateTime.now();
        this.fechaIntento = null;
        this.claimId = null;
    }

    public void marcarFallida() {
        this.estado = EstadoNotificacion.FALLIDA;
        this.fechaIntento = null;
        this.claimId = null;
    }
}
