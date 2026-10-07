package ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "chofer")
public class Chofer {
    @Id
    @Column(name = "id_chofer", nullable = false, updatable = false)
    private UUID idChofer;

    /**
     * Control de concurrencia optimista.
     *
     * <p>Sin esto, dos instancias de logistica que leen esta fila y escriben sobre ella lo
     * hacen en silencio: la segunda sobrescribe a la primera con los valores que leyo antes
     * del UPDATE de la otra. Con la version, el UPDATE lleva {@code WHERE version = ?} y si
     * otra instancia escribio en el medio Hibernate tira {@code OptimisticLockingFailureException}
     * en vez de pisar. Es lo que hace seguro el requisito de mas de un servicio de logistica.
     *
     * <p>La columna la crea sola {@code ddl-auto=update}.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "disponible", nullable = false)
    private boolean disponible;

    public Chofer(UUID idChofer, String nombre){
        this.idChofer = idChofer;
        this.nombre = nombre;
        this.disponible = true;
    }

    public Chofer(UUID idChofer, String nombre, boolean disponible){
        this.idChofer = idChofer;
        this.nombre = nombre;
        this.disponible = disponible;
    }

    public void ocupado(){
        this.disponible = false;
    }

    public void disponible(){
        this.disponible = true;
    }
}
