package ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;


import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

public class Notificacion {
    @Setter
    @Getter
    private UUID id = UUID.randomUUID();
    @Setter
    @Getter
    private Mensaje mensaje;
    @Setter
    @Getter
    private String direccionDeContacto;
    @Getter
    @Setter
    private LocalDateTime fechaCreacion;
    @Getter
    @Setter
    private LocalDateTime fechaEnvio;
    @Setter
    @Getter
    private EstadoNotificacion estado;

    public Notificacion(String direccionDeContacto, Mensaje mensaje) {
        this.fechaCreacion = LocalDateTime.now();
        this.estado = EstadoNotificacion.PENDIENTE;
        this.direccionDeContacto = direccionDeContacto;
        this.mensaje = mensaje;
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
