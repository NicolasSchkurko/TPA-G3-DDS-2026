package ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.PayloadEntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import lombok.Getter;

@Getter
public class NotificacionEntregaDTO {

    private RutaEnProceso ruta;

    private PayloadEntregaDTO datosEntrega;

    private MediosDeContacto mediosDonante;

    private MediosDeContacto mediosEntidad;

    public NotificacionEntregaDTO(PayloadEntregaDTO datosEntrega, MediosDeContacto mediosDonante, MediosDeContacto mediosEntidad) {
        this.datosEntrega = datosEntrega;
        this.mediosDonante = mediosDonante;
        this.mediosEntidad = mediosEntidad;
    }
}
