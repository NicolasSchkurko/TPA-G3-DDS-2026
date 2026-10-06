package ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * La posición de una misión dentro de la secuencia de una categoría.
 *
 * <p>Es una entidad propia y no un id compuesto porque hace falta poder preguntar "en qué
 * posición estaba esta misiónó cuando el admin reordena la secuencia.
 *
 * <p>No tiene setters: la posición solo la cambia {@link Categoria} al renumerar, y la
 * referencia a la categoría se fija en el constructor.
 */
@Getter
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

    /**
     * Cambia la posición en la secuencia. Solo {@link Categoria} lo llama, y únicamente
     * al renumerar después de sacar una misión, para que las posiciones sigan siendo
     * 1..N sin huecos.
     */
    public void moverAPosicion(Integer nuevaPosicion) {
        this.posicion = nuevaPosicion;
    }
}
