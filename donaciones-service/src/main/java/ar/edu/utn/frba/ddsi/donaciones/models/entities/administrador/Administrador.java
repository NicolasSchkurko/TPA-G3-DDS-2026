package ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter

public class Administrador {
    @Id
    private UUID id = UUID.randomUUID();

    // ManyToOne (no OneToOne): la Humana vive en su propio repositorio (RepositorioPersonas),
    // igual que Juridica para EntidadBeneficiaria. Sin cascade REMOVE: dar de baja el
    // administrador no debe borrar la Persona asociada. Se registra explícitamente vía
    // GestorPersonas antes de guardar el Administrador (ver AdminService).
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "humano_id")
    private Humana humano;

    // De uso exclusivo de este Administrador (no se comparte ni se referencia desde otro lado),
    // a diferencia de MediosDeContacto.medioDeContactoPredeterminado: acá sí podemos cascadear
    // el REMOVE sin conflicto.
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "medio_de_contacto_id")
    private MedioDeContacto medioDeContacto;

    private String nombreAMostrar;

    protected Administrador() {
        // Constructor requerido por JPA/Hibernate.
    }

    public Administrador (Humana humano,MedioDeContacto medioDeContacto,String nombreAMostrar){
        this.humano=humano;
        this.medioDeContacto=medioDeContacto;
        this.nombreAMostrar = nombreAMostrar;
    }
    public MedioDeContacto getContacto() {
        return medioDeContacto;
    }
}
