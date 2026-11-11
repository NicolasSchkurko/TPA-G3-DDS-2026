package ar.edu.utn.frba.ddsi.front.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Notificación enviada por notificaciones-service, tal como llega al front. */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificacionServicioDTO {
    private String id;
    private String asunto;
    private String cuerpo;
    private String tipoMedioDeContacto;
    private String direccionDeContacto;
    /** ISO-8601 (LocalDateTime), como lo serializa el servicio. */
    private String fechaCreacion;
    private String fechaEnvio;
    /** PENDIENTE, ENVIADA o FALLIDA. */
    private String estado;
}
