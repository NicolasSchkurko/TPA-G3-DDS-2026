package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public abstract class Persona {
    private UUID id;
    private MediosDeContacto mediosDeContacto;

    public Persona() {
        this.id = UUID.randomUUID(); // Generamos un UUID automáticamente al crear la persona
        this.mediosDeContacto = new MediosDeContacto();
    }

    public void agregarMedioDeContacto(MedioDeContacto medioDeContacto) {
        if (this.mediosDeContacto == null) {
            throw new IllegalStateException("La colección de medios de contacto no está inicializada.");
        }
        this.mediosDeContacto.agregarMedioDeContacto(medioDeContacto);
    }
    public String getNombreDeUsuario(){
      return "";
    };
}