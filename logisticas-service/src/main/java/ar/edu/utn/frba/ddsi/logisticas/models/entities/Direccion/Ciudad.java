package ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ciudad")
@Getter
@Setter
@NoArgsConstructor
public class Ciudad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ciudad")
    private Long idCiudad;

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

    @ManyToOne
    @JoinColumn(name = "id_provincia", referencedColumnName = "id_provincia", nullable = false)
    private Provincia provincia;

    public Ciudad(String nombre, Provincia provincia){
        this.nombre = nombre;
        this.provincia = provincia;
    }
}
