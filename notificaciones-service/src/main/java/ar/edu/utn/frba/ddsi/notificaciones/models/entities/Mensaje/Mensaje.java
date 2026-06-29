package ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje;

import lombok.Getter;

public class Mensaje {
    @Getter
    private String asunto;
    @Getter
    private String cuerpo;

    public Mensaje(String asunto, String cuerpo) {
        this.asunto = asunto;
        this.cuerpo = cuerpo;
    }
}
