package ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;


import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
public class Notificacion {
    private UUID id = UUID.randomUUID();
    private Mensaje mensaje;
    private String direccionDeContacto;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaEnvio;
    private EstadoNotificacion estado;
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
