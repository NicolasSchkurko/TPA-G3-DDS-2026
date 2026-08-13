package ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.PayloadEntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import lombok.Getter;

import java.util.List;

@Getter
public class NotificacionEntregaFallidaDTO {
    private RutaEnProceso ruta;
    private PayloadEntregaDTO datosEntrega;
    private MediosDeContacto contactoDonante;
    private MediosDeContacto contactoEntidad;
    private List<MedioDeContacto> contactosAdmin;

    public NotificacionEntregaFallidaDTO(PayloadEntregaDTO datosEntrega,
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
