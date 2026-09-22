package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
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

    @ManyToOne
    @JoinColumn(name = "id_parada", referencedColumnName = "id_parada", nullable = false)
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

    @OneToMany(mappedBy = "itemEntrega", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventoLogistica> eventos = new ArrayList<>();

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