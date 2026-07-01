package ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Direccion.Direccion;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Entidad {
  private UUID idEntidadBeneficiaria;
  private Direccion direccionDestino;
}