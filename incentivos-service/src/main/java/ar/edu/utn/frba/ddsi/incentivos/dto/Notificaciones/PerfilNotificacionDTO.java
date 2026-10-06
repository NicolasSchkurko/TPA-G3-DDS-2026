package ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Lo que este servicio publica en el broker para que notificaciones lo despache.
 *
 * <p><b>El nombre del campo tiene que coincidir exactamente con el del receptor.</b> Antes
 * este DTO declaraba {@code direccionContacto} y el de notificaciones declara
 * {@code direccionDeContacto}. Con Jackson el campo desalineado no da error: queda en null
 * y el INSERT muere despues con violacion de la restriccion {@code nullable = false}, en el
 * servidor de otro servicio y sin rastro de la causa.
 *
 * <p>Se corrigio el nombre en vez de agregar un alias, porque un alias invisible es
 * exactamente lo que reproduce este bug otra vez.
 *
 * <p>Implementa {@link Serializable} y tiene constructor sin argumentos porque
 * {@code Jackson2JsonMessageConverter} los necesita para deserializar del otro lado.
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