package ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DireccionEntidad {
    private String nombreEntidad;
    private Direccion direccion;
}
