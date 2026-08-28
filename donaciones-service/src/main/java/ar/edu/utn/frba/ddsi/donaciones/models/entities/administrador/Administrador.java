package ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Administrador {
    private UUID id = UUID.randomUUID();
    private Humana humano;
    private MedioDeContacto medioDeContacto;
    private String nombreAMostrar;

    public Administrador (Humana humano,MedioDeContacto medioDeContacto,String nombreAMostrar){
        this.humano=humano;
        this.medioDeContacto=medioDeContacto;
        this.nombreAMostrar = nombreAMostrar;
    }
    public MedioDeContacto getContacto() {
        return medioDeContacto;
    }
}
