package ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
public class Formulario {
    @Id
    private UUID id = UUID.randomUUID();

    // ManyToOne (no OneToOne): el Donante vive en su propio repositorio (RepositorioDonantes).
    // Sin cascade REMOVE: eliminar el formulario no debe borrar el Donante asociado.
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "donante_id")
    private Donante donante;

    // Los Bien ya se persisten individualmente vía RepositorioBienes antes de armar el
    // Formulario (ver DonacionService.procesarFormulario). Mismo criterio que Donacion.bienes:
    // cascade PERSIST/MERGE (no ALL), sin orphanRemoval, ya que cada Bien también queda referenciado
    // desde la Donacion segmentada correspondiente (columnas donacion_id/formulario_id separadas
    // en la tabla bien, sin conflicto entre sí).
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "formulario_id")
    private List<Bien> donaciones;

    private LocalDate fechaRealizacion;

    protected Formulario() {
        // Constructor requerido por JPA/Hibernate.
    }

    public Formulario(Donante donante, List<Bien> bienes, LocalDate fechaRealizacion) {
        this.donante = donante;
        this.donaciones = bienes;
        this.fechaRealizacion = fechaRealizacion;
    }
}
