package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.MediosDeContacto;
import lombok.Getter;
import lombok.Setter;

import java.util.Random;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter

public abstract class PersonaDonante {
    private long id; // Por ahora es un numero aleatorio para poder enviar algo al servicio de notificaciones
    private MediosDeContacto mediosDeContacto;
    private List<Formulario> formularios;

    Random random;

    /*public PersonaDonante(MediosDeContacto mediosDeContacto){
        this.mediosDeContacto = mediosDeContacto;
    }*/
    public PersonaDonante() {
        this.id = random.nextLong();
        this.mediosDeContacto = new MediosDeContacto();
        this.formularios = new ArrayList<>();
    }

    public void agregarMedioDeContacto(MedioDeContacto medioDeContacto) {
        this.mediosDeContacto.agregarMedioDeContacto(medioDeContacto);
    }

    public void agregarFormulario(Formulario formulario){
        this.formularios.add(formulario);
    }

    public abstract String darNombre();
}
