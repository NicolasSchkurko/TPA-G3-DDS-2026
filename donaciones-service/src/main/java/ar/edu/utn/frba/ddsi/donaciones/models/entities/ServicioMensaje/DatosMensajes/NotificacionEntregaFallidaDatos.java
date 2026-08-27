package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.DatosMensajes;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.PayloadEntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import java.util.UUID;
import lombok.Getter;

import java.util.List;

@Getter
public class NotificacionEntregaFallidaDatos {
    private UUID ruta;
    private PayloadEntregaDTO datosEntrega;
    private MediosDeContacto contactoDonante;
    private MediosDeContacto contactoEntidad;
    private List<MedioDeContacto> contactosAdmin;

    public NotificacionEntregaFallidaDatos(PayloadEntregaDTO datosEntrega,
                                           MediosDeContacto contactoDonante,
                                           MediosDeContacto contactoEntidad,
                                           List<MedioDeContacto> contactosAdmin) {
        this.ruta = ruta;
        this.datosEntrega = datosEntrega;
        this.contactoDonante = contactoDonante;
        this.contactoEntidad = contactoEntidad;
        this.contactosAdmin = contactosAdmin;
    }
}
