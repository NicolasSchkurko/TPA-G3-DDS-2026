package ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import lombok.Getter;

@Getter
public class NotificacionViajeDTO {

    private String urlRuta;

    private MediosDeContacto destinatarios;

    public NotificacionViajeDTO(String urlRuta, MediosDeContacto destinatarios) {
        this.urlRuta = urlRuta;
        this.destinatarios = destinatarios;
    }
}
