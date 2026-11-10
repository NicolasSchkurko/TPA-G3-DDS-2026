package ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
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

    // Identificador inmutable de la cuenta de Keycloak vinculada a esta entidad.
    @Column(name = "keycloak_sub", unique = true, length = 36)
    private String keycloakSub;

    // Soft delete: la entidad nunca se elimina físicamente para preservar claves foráneas y trazabilidad histórica.
    @Column(nullable = false)
    private boolean activa = true;

    private LocalDateTime fechaBaja;

    // La Direccion es de uso exclusivo de esta entidad (no se comparte entre EntidadBeneficiarias).
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "direccion_id")
    private Direccion direccion;

    // Unidireccional: Necesidad no tiene referencia de vuelta hacia EntidadBeneficiaria.
    // Sin REMOVE ni orphanRemoval: Necesidad vive en su propio repositorio (RepositorioNecesidades)
    // y está referenciada por Donacion.necesidad (@ManyToOne sin cascade).
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "entidad_beneficiaria_id")
    private List<Necesidad> necesidades = new ArrayList<>();

    // ManyToOne (no OneToOne): la Persona/Juridica vive en su propio repositorio y podría reutilizarse en otro contexto.
    // Sin cascade REMOVE: dar de baja la entidad no debe borrar la Juridica asociada.
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "juridica_id")
    private Juridica personaJuridica;

    public EntidadBeneficiaria() {
        this.id = UUID.randomUUID();
    }

    public EntidadBeneficiaria(Direccion dir, Juridica personaJuridica) {
        this.id = UUID.randomUUID();
        this.direccion = dir;
        this.necesidades = new ArrayList<>();
        this.personaJuridica = personaJuridica;
        this.activa = true;
    }

    public void darDeBaja() {
        this.activa = false;
        this.fechaBaja = LocalDateTime.now();
        if (this.necesidades != null) {
            for (Necesidad necesidad : this.necesidades) {
                necesidad.setActiva(false);
            }
        }
        if (this.personaJuridica != null) {
            this.personaJuridica.anonimizar();
        }
        if (this.direccion != null) {
            this.direccion.anonimizar();
        }
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
