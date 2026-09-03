package ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class EntidadBeneficiaria {

    @Id
    private UUID id; // Identificador único autogenerado

    // La Direccion es de uso exclusivo de esta entidad (no se comparte entre EntidadBeneficiarias).
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "direccion_id")
    private Direccion direccion;

    // Unidireccional: Necesidad no tiene referencia de vuelta hacia EntidadBeneficiaria.
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "entidad_beneficiaria_id")
    private List<Necesidad> necesidades = new ArrayList<>();

    // ManyToOne (no OneToOne): la Persona/Juridica vive en su propio repositorio y podría reutilizarse en otro contexto.
    // Sin cascade REMOVE: dar de baja la entidad no debe borrar la Juridica asociada.
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "juridica_id")
    private Juridica personaJuridica;

    protected EntidadBeneficiaria() {
        // Constructor requerido por JPA/Hibernate.
    }

    public EntidadBeneficiaria(Direccion dir, Juridica personaJuridica) {
        this.direccion = dir;
        this.necesidades = new ArrayList<>();
        this.personaJuridica = personaJuridica;
        this.id = UUID.randomUUID();
    }

    public void agregarNecesidad(Necesidad necesidad) {
        this.necesidades.add(necesidad);
    }

    public void eliminarNecesidad(Necesidad necesidad) {
        this.necesidades.remove(necesidad);
    }

    public Optional<Necesidad> buscarNecesidadPorId(UUID idNecesidad) {
        return necesidades.stream()
                          .filter(n -> n.getId().equals(idNecesidad))
                          .findFirst();
    }

    public List<Donacion> verDonaciones() {
        return necesidades.stream()
                          .flatMap(necesidad -> necesidad.getDonaciones().stream())
                          .toList();
    }


}