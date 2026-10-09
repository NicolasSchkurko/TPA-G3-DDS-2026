package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "item_entrega")
@Getter
@Setter
@NoArgsConstructor
public class ItemEntrega {
    @Id
    @Column(name = "id_donacion", nullable = false, updatable = false)
    private UUID idDonacion;

    /**
     * Control de concurrencia optimista: el UPDATE lleva {@code WHERE version = ?} y si otra
     * instancia escribió en el medio Hibernate tira {@code OptimisticLockingFailureException} en
     * vez de pisar. Hace falta sobre todo porque {@code idDonacion} es clave natural, así que
     * {@code save()} va siempre por {@code merge()}.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * La parada a la que pertenece el ítem. Se ignora al serializar porque
     * {@code item.parada} → {@code parada.ruta} → {@code ruta.paradas} vuelve al mismo objeto y
     * Jackson reventaría con {@code StackOverflowError}.
     */
    @ManyToOne
    @JoinColumn(name = "id_parada", referencedColumnName = "id_parada")
    @JsonIgnore
    private Parada parada;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @ManyToOne
    @JoinColumn(name = "id_unidad_medida", referencedColumnName = "id_unidad", nullable = false)
    private UnidadDeMedida unidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoEntrega estado;

    @Column(name = "fecha_cambio_estado", nullable = false)
    private LocalDateTime fechaCambioEstado;

    @Column(name = "foto_comprobante")
    private String fotoComprobante; // URL de la foto cargada por la entidad al confirmar recepción

    @ManyToOne
    @JoinColumn(name = "id_entidad_beneficiaria")
    private Entidad entidadDestino;

    /**
     * Los eventos de trazabilidad de este ítem: el lado inverso de la relación.
     *
     * <p>El {@code cascade} es lo que hace que el ítem se pueda borrar: MySQL rechaza el
     * {@code DELETE} de un ítem con eventos, así que el remove borra primero los hijos.
     */
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventoLogistica> eventos;

    public ItemEntrega(UUID idDonacion, Integer cantidad, UnidadDeMedida unidad, Entidad entidadDestino) {
        this.idDonacion = idDonacion;
        this.cantidad = cantidad;
        this.unidad = unidad;
        this.entidadDestino = entidadDestino;
        this.estado = EstadoEntrega.PENDIENTE;
        this.fechaCambioEstado = LocalDateTime.now();
        this.eventos = new ArrayList<>();
    }

    // Peso y volumen no se guardan, se calculan siempre a partir de cantidad+unidad.
    public Double getPesoEstimadoKg() {
        return unidad.calcularPesoKg(cantidad);
    }

    public Double getVolumenEstimadoM3() {
        return unidad.calcularVolumenM3(cantidad);
    }
}