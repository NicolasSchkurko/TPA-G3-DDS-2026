package ar.edu.utn.frba.ddsi.notificaciones.models.entities.GestorNotificacion;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.CanalNotificacion.CanalNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.TipoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Destinatario.Destinatario;

import java.util.Map;

public class GestorNotificacion {

    public Notificacion crearNotificacion(TipoNotificacion tipo, Destinatario destinatario, Map datos) {
        Notificacion notificacion = new Notificacion();
        notificacion.setDestinatario(destinatario);
        notificacion.setTipo(tipo);
        notificacion.setFechaCreacion(java.time.LocalDateTime.now());
        notificacion.setEstado(EstadoNotificacion.PENDIENTE);
        // notificacion.setAsunto(Map.asunto())
        // notificacion.setCuerpo(Map.cuerpo())
        return notificacion;
    }

    public void enviarNotificacion(Notificacion notificacion, CanalNotificacion canalDeEnvio) {
        boolean enviado = canalDeEnvio.enviar(notificacion, notificacion.getDestinatario());
        if (enviado) {
            notificacion.marcarEnviada();
        } else {
            notificacion.marcarFallida();
        }
    }

    public void crearYEnviarNotificacion(TipoNotificacion tipo, Destinatario destinatario, Map datos, CanalNotificacion canalDeEnvio) {
        Notificacion notificacion = this.crearNotificacion(tipo, destinatario, datos);
        this.enviarNotificacion(notificacion, canalDeEnvio);
    }
}
