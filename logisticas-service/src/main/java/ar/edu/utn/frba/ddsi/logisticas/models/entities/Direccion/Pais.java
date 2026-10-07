package ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pais")
@Getter
@Setter
@NoArgsConstructor // Requerido por JPA
public class Pais {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pais")
    private Long idPais;

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
}
