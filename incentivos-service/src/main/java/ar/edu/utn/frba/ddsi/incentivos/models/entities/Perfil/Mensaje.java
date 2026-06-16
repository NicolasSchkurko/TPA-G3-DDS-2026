package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Mensaje {
    private String cuerpoMensaje;
    private String asuntoMensaje;

    public Mensaje(String cuerpoMensaje,
                   String asuntoMensaje){
        this.cuerpoMensaje = cuerpoMensaje;
        this.asuntoMensaje = asuntoMensaje;
    }
}
