package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.DatosMensajes;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import lombok.Getter;

@Getter
public class NotificacionViajeDatos {

    private String urlRuta;

    private MediosDeContacto contactoDonante;

    private MediosDeContacto contactoEntidad;

    public NotificacionViajeDatos(String urlRuta, MediosDeContacto destinatarios, MediosDeContacto contactoEntidad) {
        this.urlRuta = urlRuta;
        this.contactoDonante = destinatarios;
        this.contactoEntidad = contactoEntidad;
    }
}
