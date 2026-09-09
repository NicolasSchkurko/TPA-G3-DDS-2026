package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class MetricaDonacionesDTO {
    private UUID idUsuario;
    private Long cantidadDonaciones;
    private Long cantidadBienes;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private List<String> entidadesBeneficiarias;
}
