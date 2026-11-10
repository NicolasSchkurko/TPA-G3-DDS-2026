package ar.edu.utn.frba.ddsi.notificaciones.dto.Notificaciones;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificacionDTO {
    private String id;
    private String asunto;
    private String cuerpo;
    private String urlMapa;
    private String tipoMedioDeContacto;
    private String direccionDeContacto;
    private String fechaCreacion;
    private String fechaEnvio;
    private String estado;
}
