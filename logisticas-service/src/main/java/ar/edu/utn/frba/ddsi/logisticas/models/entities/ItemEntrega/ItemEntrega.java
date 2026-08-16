package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.Estado.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.Estado.Pendiente;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemEntrega {
    private UUID idDonacion;
    private Integer cantidad;
    private UnidadDeMedida unidad;
    private EstadoEntrega estado;
    private LocalDateTime fechaCambioEstado;
    private String fotoComprobante; // URL de la foto cargada por la entidad al confirmar recepción
    private Entidad entidadDestino;
    private List<EventoLogistica> eventos;

    public ItemEntrega(UUID idDonacion, Integer cantidad, UnidadDeMedida unidad, Entidad entidadDestino) {
        this.idDonacion = idDonacion;
        this.cantidad = cantidad;
        this.unidad = unidad;
        this.entidadDestino = entidadDestino;
        this.estado = new Pendiente(this);
        this.fechaCambioEstado = LocalDateTime.now();
    }

    // Peso y volumen no se guardan, se calculan siempre a partir de cantidad+unidad.
    public Double getPesoEstimadoKg() {
        return unidad.calcularPesoKg(cantidad);
    }

    public Double getVolumenEstimadoM3() {
        return unidad.calcularVolumenM3(cantidad);
    }

    public void confirmarEntrega(String fotoComprobante) {
        if (estado != EstadoEntrega.EN_TRASLADO) {
            throw new IllegalStateException("Solo se puede confirmar entrega desde EN_TRASLADO, estado actual: " + estado);
        }
        this.fotoComprobante = fotoComprobante;
        cambiarEstado(EstadoEntrega.ENTREGADA);
    }

    public void marcarNoRecibida() {
        if (estado != EstadoEntrega.EN_TRASLADO) {
            throw new IllegalStateException("Solo se puede marcar no recibida desde EN_TRASLADO, estado actual: " + estado);
        }
        cambiarEstado(EstadoEntrega.NO_RECIBIDA);
    }

    public void reingresarADeposito() {
        if (estado != EstadoEntrega.NO_RECIBIDA) {
            throw new IllegalStateException("Solo puede reingresar desde NO_RECIBIDA, estado actual: " + estado);
        }
        cambiarEstado(EstadoEntrega.PENDIENTE);
    }

    public void cambiarEstado(EstadoEntrega nuevoEstado) {
        this.estado = nuevoEstado;
        this.fechaCambioEstado = LocalDateTime.now();
    }
}