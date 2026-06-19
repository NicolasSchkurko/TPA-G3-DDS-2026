package ar.edu.utn.frba.ddsi.incentivos.dto;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter

public class PerfilNotificacionDTO {
    private String medioDeContacto;
    private String direccionContacto;
    private String cuerpoMensaje;
    private String asuntoMensaje;

    public PerfilNotificacionDTO(String medioDeContacto,
                            String direccionContacto,
                                 String cuerpoMensaje,
                                 String asuntoMensaje){
        this.medioDeContacto = medioDeContacto;
        this.direccionContacto = direccionContacto;
        this.cuerpoMensaje = cuerpoMensaje;
        this.asuntoMensaje = asuntoMensaje;
    }
}