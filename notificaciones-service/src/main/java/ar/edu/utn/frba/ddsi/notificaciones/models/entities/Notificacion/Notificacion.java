package ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "notificaciones")
@NoArgsConstructor
public class Notificacion {

    @Id
    private UUID id = UUID.randomUUID();

    /** Cascade ALL y no PERSIST: el repo siempre va por merge() porque el id se asigna a mano. */
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

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoNotificacion estado;

    @Column(name = "tipo_medio_contacto", nullable = false)
    private String tipoMedioDeContacto;

    public Notificacion(String direccionDeContacto, String tipoMedioDeContacto, Mensaje mensaje) {
        this.fechaCreacion = LocalDateTime.now();
        this.estado = EstadoNotificacion.PENDIENTE;
        this.direccionDeContacto = direccionDeContacto;
        this.tipoMedioDeContacto = tipoMedioDeContacto;
        this.mensaje = mensaje;
    }

    public void marcarEnviada() {
        this.estado = EstadoNotificacion.ENVIADA;
        this.fechaEnvio = LocalDateTime.now();
    }

    public void marcarFallida() {
        this.estado = EstadoNotificacion.FALLIDA;
    }
}
