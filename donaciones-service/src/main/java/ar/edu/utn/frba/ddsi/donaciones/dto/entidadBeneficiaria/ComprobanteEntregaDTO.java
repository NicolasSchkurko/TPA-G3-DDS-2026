package ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class ComprobanteEntregaDTO {
    private String entregaSatisfactoria; //quiza sea otro tipo de dato
    private List<String> urlImagenesEntrega;
    private LocalDate fechaEntrega;
    private LocalTime horaEntrega;
    private String nombreChofer;
    private String patente;
}
