package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class Persona {

    @Id
    private UUID id;

    // Cada Persona tiene su propia colección de medios de contacto (no se comparte con otras Personas).
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "medios_de_contacto_id")
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

    public void eliminarMedioDeContacto(MedioDeContacto medioDeContacto) {
        if (this.mediosDeContacto == null) {
            throw new IllegalStateException("La colección de medios de contacto no existe.");
        }
        this.mediosDeContacto.eliminarMedioDeContacto(medioDeContacto);
    }

    public String getNombreDeUsuario(){
      return "";
    };
}