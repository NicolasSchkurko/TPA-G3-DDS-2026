package ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RutaEnProceso {
    private UUID idRuta;
    private UUID idEntrega;
    private Entrega paquete;//PAQUETE A ENTREGARLE (donaciones)
    private Camion camionEntrega;//camion ASIGNADO
    private String urlSeguimiento;
    private EstadoEntrega estadoEntrega;
    private List<String> urlImagenesEntrega;
    private LocalDate fechaEntrega;
    private LocalTime horaEntrega;
}
