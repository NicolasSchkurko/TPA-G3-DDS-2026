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

    /**
     * Control de concurrencia optimista.
     *
     * <p><b>Es lo que hace seguro correr N instancias sobre la misma base.</b> Sin esto, dos
     * instancias que leen el mismo item y escriben sobre el lo hacen en silencio: la segunda
     * sobrescribe a la primera con los valores que leyo <i>antes</i> del UPDATE de la otra, y
     * no hay excepcion, no hay log, no queda rastro. Con la version, el UPDATE lleva
     * {@code WHERE version = ?}; si otra instancia escribio en el medio, la cantidad de filas
     * afectadas es cero y Hibernate tira {@code OptimisticLockingFailureException} en vez de
     * pisar.
     *
     * <p>En esta entidad es especialmente importante porque {@code idDonacion} es clave natural
     * sin {@code @GeneratedValue}: {@code save()} va siempre por {@code merge()}, asi que el
     * UPDATE silencioso era el camino normal y no la excepcion. Y como {@code estado} y
     * {@code fechaCambioEstado} los cambia el operador de logistica por HTTP mientras el
     * listener de Rabbit puede estar registrando la misma donacion, la carrera es real.
     *
     * <p>La columna la crea sola {@code ddl-auto=update}.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne
    @JoinColumn(name = "id_parada", referencedColumnName = "id_parada")
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

    @OneToMany(mappedBy = "id", cascade = CascadeType.ALL, orphanRemoval = true)
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