package ar.edu.utn.frba.ddsi.logisticas.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DestinoEntregaDTO {//pasar a donaciones serv
    private UUID idRuta;
    private EntregaDTO paquete;//PAQUETE A ENTREGARLE (donaciones)
    private CamionDTO camionEntrega; //camion ASIGNADO
    private String urlSeguimiento; //URL CON LA RUTA PARA EL SEGUIMIENTO (n8nClient para conectar la ruta y q la persona pueda hacer el seguimiento)
}