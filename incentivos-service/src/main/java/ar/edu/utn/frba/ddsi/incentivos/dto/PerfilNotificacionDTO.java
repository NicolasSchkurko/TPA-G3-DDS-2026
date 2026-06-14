package ar.edu.utn.frba.ddsi.incentivos.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter

public class PerfilNotificacionDTO {
    private String servicioOrigen;
    private String medioDeContacto;
    private String direccionContacto;
    private String cuerpoMensaje;
    private String asuntoMensaje;
}