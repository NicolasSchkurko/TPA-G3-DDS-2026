package ar.edu.utn.frba.ddsi.notificaciones.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificacionDTO {
    private String asunto;
    private String cuerpo;
    private String direccionDeContacto;
    private String fechaCreacion;
    private String fechaEnvio;
    private String estado;
}
