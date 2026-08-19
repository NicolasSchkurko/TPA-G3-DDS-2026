package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.DatosMensajes;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.PayloadEntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import lombok.Getter;

@Getter
public class NotificacionEntregaFallidaAdminDatos {
    private RutaEnProceso ruta;
    private PayloadEntregaDTO datosEntrega;
    private MedioDeContacto contactoAdmin;

    public NotificacionEntregaFallidaAdminDatos(PayloadEntregaDTO datosEntrega, MedioDeContacto contacto) {
        this.ruta = ruta;
        this.datosEntrega = datosEntrega;
        this.contactoAdmin = contacto;
    }
}
