package ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;


import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
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
    @OneToOne
    @JoinColumn(name = "id_mensaje", referencedColumnName = "id_mensaje")
    private Mensaje mensaje;
    @Column(name = "direccionDeContacto", nullable = false)
    private String direccionDeContacto;
    @Column(name = "fechaCreacion", nullable = false)
    private LocalDateTime fechaCreacion;
    @Column(name = "fechaEnvio", nullable = false)
    private LocalDateTime fechaEnvio;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoNotificacion estado;
    @Column(name = "tipoMedioDeContacto", nullable = false)
    private String tipoMedioDeContacto;


    public Notificacion(String direccionDeContacto, Mensaje mensaje) {
        this.fechaCreacion = LocalDateTime.now();
        this.estado = EstadoNotificacion.PENDIENTE;
        this.direccionDeContacto = direccionDeContacto;
        this.mensaje = mensaje;
        this.tipoMedioDeContacto = tipoMedioDeContacto;
    }

    public void marcarEnviada() {
        this.estado = EstadoNotificacion.ENVIADA;
    }

    public void marcarFallida() {
        this.estado = EstadoNotificacion.FALLIDA;
    }

    public void marcarPendiente() {
        this.estado = EstadoNotificacion.PENDIENTE;
    }


}
