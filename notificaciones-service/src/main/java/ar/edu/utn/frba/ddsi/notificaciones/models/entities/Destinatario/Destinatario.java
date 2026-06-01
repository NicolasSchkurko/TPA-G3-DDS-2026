package ar.edu.utn.frba.ddsi.notificaciones.models.entities.Destinatario;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeContacto.MediosDeContacto;
import lombok.Getter;
import lombok.Setter;

public class Destinatario {
    @Getter
    @Setter
    private Number idDestinatario;
    @Getter
    @Setter
    private String nombre;
    private MediosDeContacto mediosDeContacto;

    public Destinatario(String nombre, MediosDeContacto mediosDeContacto){
        this.nombre = nombre;
        this.mediosDeContacto = mediosDeContacto;
    }
}
