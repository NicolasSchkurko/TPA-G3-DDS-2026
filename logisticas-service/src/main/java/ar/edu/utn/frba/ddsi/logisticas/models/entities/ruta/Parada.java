package ar.edu.utn.frba.ddsi.logisticas.models.entities.ruta;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.itemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ruta.direccion.Direccion;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Parada {
    private UUID idEntidadBeneficiaria;
    private Direccion direccionDestino;
    private ItemEntrega item;
}