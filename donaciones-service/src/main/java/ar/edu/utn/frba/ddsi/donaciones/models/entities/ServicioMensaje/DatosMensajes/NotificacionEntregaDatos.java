package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.DatosMensajes;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.PayloadEntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import java.util.UUID;
import lombok.Getter;

@Getter
public class NotificacionEntregaDatos {

    private UUID ruta;

    private PayloadEntregaDTO datosEntrega;

    private MediosDeContacto mediosDonante;

    private MediosDeContacto mediosEntidad;

    public NotificacionEntregaDatos(PayloadEntregaDTO datosEntrega, MediosDeContacto mediosDonante, MediosDeContacto mediosEntidad) {
        this.datosEntrega = datosEntrega;
        this.mediosDonante = mediosDonante;
        this.mediosEntidad = mediosEntidad;
    }
}
