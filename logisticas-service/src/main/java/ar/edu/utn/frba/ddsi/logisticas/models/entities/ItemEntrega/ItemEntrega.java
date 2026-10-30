package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "item_entrega")
@Getter
@Setter
@NoArgsConstructor
public class ItemEntrega {
    @Id
    @Field(name = "id_donacion")
    private UUID idDonacion;

    /**
     * Control de concurrencia optimista: el UPDATE lleva {@code WHERE version = ?} y si otra
     * instancia escribió en el medio Hibernate tira {@code OptimisticLockingFailureException} en
     * vez de pisar. Hace falta sobre todo porque {@code idDonacion} es clave natural, así que
     * {@code save()} va siempre por {@code merge()}.
     */
    @Version
    private Long version;

    /**
     * La parada a la que pertenece el ítem. Se ignora al serializar porque
     * {@code item.parada} → {@code parada.ruta} → {@code ruta.paradas} vuelve al mismo objeto y
     * Jackson reventaría con {@code StackOverflowError}.
     */

    @Field(name = "cantidad")
    private Integer cantidad;

    private UnidadDeMedida unidad;

    @Field(name = "estado")
    private EstadoEntrega estado;

    @Field(name = "fecha_cambio_estado")
    private LocalDateTime fechaCambioEstado;

    @Field(name = "foto_comprobante")
    private String fotoComprobante; // URL de la foto cargada por la entidad al confirmar recepción

    private Entidad entidadDestino;

    /**
     * Los eventos de trazabilidad de este ítem: el lado inverso de la relación.
     *
     * <p>El {@code cascade} es lo que hace que el ítem se pueda borrar: MySQL rechaza el
     * {@code DELETE} de un ítem con eventos, así que el remove borra primero los hijos.
     */
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