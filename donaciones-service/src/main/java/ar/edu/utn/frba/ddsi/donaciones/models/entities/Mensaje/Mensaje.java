package ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Mensaje {
    private String asunto;
    private String cuerpo;
    private TipoDeMensaje tipoDeMensaje;
    /** Opcional: link estructurado a un mapa/adjunto, para no dejarlo solo pegado en el cuerpo. */
    private String urlMapa;

    public Mensaje(String asunto, String cuerpo, TipoDeMensaje tipoDeMensaje) {
        this.asunto = asunto;
        this.cuerpo = cuerpo;
        this.tipoDeMensaje = tipoDeMensaje;
    }

    public String getTexto(){
        return "Asunto: " + asunto + "\n" +
               "Cuerpo: " + cuerpo + "\n";
    }
}
