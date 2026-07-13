package ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.PayloadEntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import lombok.Getter;

@Getter
public class NotificacionEntregaAdminDTO {
    private RutaEnProceso ruta;
    private PayloadEntregaDTO datosEntrega;
    private MedioDeContacto contactoAdmin;

    public NotificacionEntregaDTO(PayloadEntregaDTO datosEntrega, MedioDeContacto contacto) {
        this.ruta = ruta;
        this.datosEntrega = datosEntrega;
        this.contactoAdmin = contacto;
    }
}
