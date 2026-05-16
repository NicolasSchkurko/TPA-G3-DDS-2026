package ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.CanalNotificacion.CanalNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Destinatario.Destinatario;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class Notificacion {
    private Number idNotificacion;
    @Setter
    private String asunto;
    private String cuerpo;
    @Setter
    @Getter
    private Destinatario destinatario;
    @Setter
    private LocalDateTime fechaCreacion;
    @Setter
    private EstadoNotificacion estado;
    @Setter
    private TipoNotificacion tipo;
    private CanalNotificacion canalDeEnvio;

    public void marcarEnviada(){
        this.estado = EstadoNotificacion.ENVIADA;
    }

    public void marcarFallida(){
        this.estado = EstadoNotificacion.FALLIDA;
    }

}
