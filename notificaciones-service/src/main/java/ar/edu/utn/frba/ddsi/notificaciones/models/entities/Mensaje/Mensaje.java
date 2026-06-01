package ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje;

public class Mensaje {
    private String asunto;
    private String cuerpo;

    public Mensaje(String asunto, String cuerpo) {
        this.asunto = asunto;
        this.cuerpo = cuerpo;
    }
}
