package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor // Requerido por JPA
@Entity
public class CategoriaMision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // FetchType.LAZY evita que traiga la categoría entera cuando consultamos misiones
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    // FetchType.LAZY evita que traiga la misión pesada (con sus reglas e insignias) a menos que se necesite
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mision_id")
    private Mision mision;

    private Integer posicion;

    public CategoriaMision(Categoria categoria, Mision mision, Integer posicion) {
        this.categoria = categoria;
        this.mision = mision;
        this.posicion = posicion;
    }
}