package ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Destinatario.Destinatario;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class Notificacion {
    @Setter
    private Mensaje mensaje;
    @Setter
    @Getter
    private Destinatario destinatario;
    @Setter
    private LocalDateTime fechaCreacion;
    @Setter
    private LocalDateTime fechaEnvio;
    @Setter
    private EstadoNotificacion estado;
    @Setter
    @Getter
    private TipoNotificacion tipo;

    public Notificacion(TipoNotificacion tipo, Destinatario destinatario, Mensaje mensaje) {
        this.fechaCreacion = LocalDateTime.now();
        this.estado = EstadoNotificacion.PENDIENTE;
        this.tipo = tipo;
        this.destinatario = destinatario;
        this.mensaje = mensaje;
    }

    public void marcarEnviada(){
        this.estado = EstadoNotificacion.ENVIADA;
    }

    public void marcarFallida(){
        this.estado = EstadoNotificacion.FALLIDA;
    }

}
