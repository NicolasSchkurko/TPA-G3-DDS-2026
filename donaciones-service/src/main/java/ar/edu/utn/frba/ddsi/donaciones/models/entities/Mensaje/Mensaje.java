package ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Mensaje {


    private String asunto;
    private String cuerpo;
    private TipoDeMensaje tipoDeMensaje;
}
