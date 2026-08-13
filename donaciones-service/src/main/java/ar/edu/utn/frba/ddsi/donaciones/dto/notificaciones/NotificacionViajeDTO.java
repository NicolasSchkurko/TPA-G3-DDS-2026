package ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import lombok.Getter;

@Getter
public class NotificacionViajeDTO {

    private String urlRuta;

    private MediosDeContacto contactoDonante;

    private MediosDeContacto contactoEntidad;

    public NotificacionViajeDTO(String urlRuta, MediosDeContacto destinatarios, MediosDeContacto contactoEntidad) {
        this.urlRuta = urlRuta;
        this.contactoDonante = destinatarios;
        this.contactoEntidad = contactoEntidad;
    }
}
