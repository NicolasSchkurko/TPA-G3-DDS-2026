package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.MediosDeContacto;
import lombok.Getter;
import lombok.Setter;

import java.util.Random;

@Getter
@Setter

public abstract class PersonaDonante {
    private long id; // Por ahora es un numero aleatorio para poder enviar algo al servicio de notificaciones
    private MediosDeContacto mediosDeContacto;

    Random random;

    /*public PersonaDonante(MediosDeContacto mediosDeContacto){
        this.mediosDeContacto = mediosDeContacto;
    }*/
    public PersonaDonante() {
        this.id = random.nextLong();
        this.mediosDeContacto = new MediosDeContacto();
    }

    public void agregarMedioDeContacto(MedioDeContacto medioDeContacto) {
        this.mediosDeContacto.agregarMedioDeContacto(medioDeContacto);
    }

    public abstract String darNombre();
}
