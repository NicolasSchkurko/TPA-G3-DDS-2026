package ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DireccionEntidad {
    private UUID idEntidad;
    private Direccion direccion;
}
