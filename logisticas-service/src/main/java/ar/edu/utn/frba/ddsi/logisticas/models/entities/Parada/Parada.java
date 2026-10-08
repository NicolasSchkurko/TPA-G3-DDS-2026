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

    /** Optimistic locking: una escritura concurrente tira OptimisticLockingFailureException en vez de pisar. */
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
    private Entidad entidadDestino;

    @OneToMany(mappedBy = "parada")
    private List<ItemEntrega> items = new ArrayList<>();

    // Se crea siempre a partir de un primer itm y su dirección
    public Parada(ItemEntrega primerItem) {
        Entidad entidad = primerItem.getEntidadDestino();
        this.entidadDestino = entidad;
        this.direccion = entidad.getDireccionDestino();
        this.agregarItem(primerItem);
    }

    public void agregarItem(ItemEntrega item) {
        items.add(item);
    }
}