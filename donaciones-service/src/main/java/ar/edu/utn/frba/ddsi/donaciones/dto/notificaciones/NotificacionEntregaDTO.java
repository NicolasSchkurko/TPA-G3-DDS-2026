package ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.PayloadEntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import lombok.Getter;

import java.util.List;

@Getter
public class NotificacionEntregaDTO {

    private RutaEnProceso ruta;

    private PayloadEntregaDTO datosEntrega;

    private MediosDeContacto destinatarios;

    public NotificacionEntregaDTO(PayloadEntregaDTO datosEntrega, MediosDeContacto destinatarios) {
        this.datosEntrega = datosEntrega;
        this.destinatarios = destinatarios;
    }
}
