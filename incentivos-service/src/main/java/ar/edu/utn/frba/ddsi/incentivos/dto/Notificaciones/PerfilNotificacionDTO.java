package ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Lo que este servicio publica en el broker para notificaciones. Los nombres de los campos
 * tienen que coincidir con los del receptor. {@code Serializable} y constructor sin
 * argumentos porque los necesita {@code Jackson2JsonMessageConverter}.
 */
@Getter
@Setter
@NoArgsConstructor
public class PerfilNotificacionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String medioDeContacto;

    private String direccionDeContacto;

    private String cuerpoMensaje;

    private String asuntoMensaje;

    public PerfilNotificacionDTO(String medioDeContacto,
                                 String direccionDeContacto,
                                 String cuerpoMensaje,
                                 String asuntoMensaje) {
        this.medioDeContacto = medioDeContacto;
        this.direccionDeContacto = direccionDeContacto;
        this.cuerpoMensaje = cuerpoMensaje;
        this.asuntoMensaje = asuntoMensaje;
    }
}