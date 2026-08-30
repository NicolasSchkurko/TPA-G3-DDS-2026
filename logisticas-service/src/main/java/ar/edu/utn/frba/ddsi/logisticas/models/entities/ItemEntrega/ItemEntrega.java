package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
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