package ar.edu.utn.frba.ddsi.logisticas.dto;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.itemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.itemEntrega.UnidadDeMedida;

import java.time.LocalDateTime;
import java.util.UUID;

public class ItemEntregaDTO {
    private UUID idDonacion;
    private Integer cantidad;
    private UnidadDeMedida unidad;
    private EstadoEntrega estado;
    private LocalDateTime fechaCambioEstado;
}
