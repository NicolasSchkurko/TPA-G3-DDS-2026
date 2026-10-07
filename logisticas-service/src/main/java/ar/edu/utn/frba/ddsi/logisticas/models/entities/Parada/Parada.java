package ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parada")
@Getter
@Setter
@NoArgsConstructor
public class Parada {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_parada", nullable = false, updatable = false)
    private UUID idParada;

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

    @ManyToOne
    @JoinColumn(name = "id_ruta", referencedColumnName = "id_ruta", nullable = false)
    private Ruta ruta;

    @ManyToOne
    @JoinColumn(name = "id_direccion", referencedColumnName = "id_direccion", nullable = false)
    private Direccion direccion;

    @ManyToOne
    @JoinColumn(name = "id_entidad_beneficiaria", referencedColumnName = "id_entidad_beneficiaria", nullable = false)
    private Entidad entidadDestino; //quedo raro porque hay un metodo que te da la entidad pero creo que es necesario pala la DB

    @OneToMany(mappedBy = "parada")
    private List<ItemEntrega> items = new ArrayList<>();

    // Se crea siempre a partir de un primer item y su dirección
    public Parada(ItemEntrega primerItem) {
        Entidad entidad = primerItem.getEntidadDestino();
        this.entidadDestino = entidad;
        this.direccion = entidad.getDireccionDestino();
        this.agregarItem(primerItem);
    }

    public void agregarItem(ItemEntrega item) {
        items.add(item);
    }

    /**
     * La entidad a la que lleva el primer item de la parada, o {@code null} si no tiene ninguno.
     *
     * <p><b>No puede usar {@code getFirst()}.</b> Con Java 21, {@code List.getFirst()} sobre una
     * lista vacia tira {@code NoSuchElementException}, y eso no loitte en un log: revienta el
     * endpoint entero.
     *
     * <p>Se vio con {@code GET /api/rutas}: una parada puede quedar sin items (la entrega se
     * elimino, o la ruta se planifico y todavia no se le asigno nada), y con una sola parada asi
     * el listado de rutas devolvia 500 y no habia forma de ver las otras.
     *
     * <p>Devolver {@code null} es lo que corresponde: "esta parada no tiene destino todavia" es
     * un estado valido de una parada a medio planificar, no un error.
     */
    public Entidad getEntidadDestino() {
        return items.isEmpty() ? null : items.getFirst().getEntidadDestino();
    }
}
