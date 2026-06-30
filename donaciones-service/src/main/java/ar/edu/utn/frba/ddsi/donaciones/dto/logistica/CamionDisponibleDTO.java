package ar.edu.utn.frba.ddsi.donaciones.dto.logistica;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CamionDisponibleDTO {
    //me parece que deberia poner los datos del chofer tmb
    private String nombreChofer;
    private String patente;
    private Integer capacidadVolumen; //m2
    private Integer altura; //m
    private Integer capacidadCarga; //kg
}
