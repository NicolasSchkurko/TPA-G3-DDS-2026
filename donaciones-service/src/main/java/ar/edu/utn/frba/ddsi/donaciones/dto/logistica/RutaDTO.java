package ar.edu.utn.frba.ddsi.donaciones.dto.logistica;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RutaDTO {
    private DireccionEntidadDTO entidadEntrega;//DIRECCION DE LA ENTIDAD BENEFICIARIA
    private EntregaDTO paquete;//PAQUETE A ENTREGARLE (donaciones)
    private CamionDisponibleDTO camionEntrega;//camion ASIGNADO
    private String urlSeguimeinto; //URL CON LA RUTA PARA EL SEGUIMIENTO (n8nClient para conectar la ruta y q la persona pueda hacer el seguimiento)
}
