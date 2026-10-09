package ar.edu.utn.frba.ddsi.donaciones.models.entities.donador;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Persona;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Donante {
    @Id
    private UUID id;

    // Lado inverso: el dueño de la relación es Formulario.donante (@JoinColumn donante_id).
    // Sin cascade acá: agregar/quitar un Formulario de esta lista no debe alterar su ciclo de
    // vida. Antes estaba @Transient: nunca se cargaba desde la base (obtenerTodos() siempre
    // traía la lista vacía) y agregarFormularioADonante() no persistía nada.
    @OneToMany(mappedBy = "donante")
    private List<Formulario> formularios = new ArrayList<>();

    // La Direccion es de uso exclusivo de este Donante (no se comparte entre Donantes), igual
    // que EntidadBeneficiaria.direccion.
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "direccion_id")
    private Direccion direccion;

    // ManyToOne (no OneToOne): la Persona vive en su propio repositorio (RepositorioPersonas),
    // igual que Administrador.humano / EntidadBeneficiaria.personaJuridica. Sin cascade REMOVE:
    // dar de baja el donante no debe borrar la Persona asociada. Se registra explícitamente vía
    // RepositorioPersonas antes de guardar el Donante (ver DonanteService).
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "persona_id")
    private Persona persona;

    protected Donante() {
        // Constructor requerido por JPA/Hibernate.
    }

    public Donante(Direccion direccion, Persona persona ) {
        this.id = UUID.randomUUID();
        this.formularios = new ArrayList<>();
        this.direccion = direccion;
        this.persona = persona;
    }



    public void agregarFormulario(Formulario formulario){
        this.formularios.add(formulario);
    }
}