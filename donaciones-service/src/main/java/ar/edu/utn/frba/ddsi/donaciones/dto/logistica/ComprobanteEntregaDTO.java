package ar.edu.utn.frba.ddsi.donaciones.dto.logistica;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComprobanteEntregaDTO {
    private String urlSeguimiento; //URL CON LA RUTA PARA EL SEGUIMIENTO (n8nClient para conectar la ruta y q la persona pueda hacer el seguimiento)
    private LocalDate fechaEntrega;
    private LocalTime horaEntrega;
    private CamionDisponibleDTO camionAsignado;
}
