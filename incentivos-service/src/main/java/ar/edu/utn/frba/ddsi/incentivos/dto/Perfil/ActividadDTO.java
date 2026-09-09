package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ActividadDTO {
    private List<RegistroMensualDTO> registros;
    private Long totalDonaciones;
    private Long totalOrganizaciones;
}
