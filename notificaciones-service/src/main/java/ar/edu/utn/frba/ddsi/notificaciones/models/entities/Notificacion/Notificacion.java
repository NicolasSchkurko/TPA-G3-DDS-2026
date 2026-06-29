package ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;


import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class Notificacion {
    @Setter
    @Getter
    private Mensaje mensaje;
    @Setter
    @Getter
    private String direccionDeContacto;
    @Setter
    private LocalDateTime fechaCreacion;
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

    public void marcarEnviada(){
        this.estado = EstadoNotificacion.ENVIADA;
    }

    public void marcarFallida(){
        this.estado = EstadoNotificacion.FALLIDA;
    }

}
