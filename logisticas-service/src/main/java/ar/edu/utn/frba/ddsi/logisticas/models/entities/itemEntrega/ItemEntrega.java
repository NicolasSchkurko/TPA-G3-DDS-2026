package ar.edu.utn.frba.ddsi.logisticas.models.entities.itemEntrega;

import java.time.LocalDateTime;
import java.util.UUID;

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
}
