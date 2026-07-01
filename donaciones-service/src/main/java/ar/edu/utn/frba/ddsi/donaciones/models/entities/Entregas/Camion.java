package ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Camion {
    private String nombreChofer;
    private String patente;
    private String inicioRuta;
}
