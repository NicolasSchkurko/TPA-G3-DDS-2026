package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ActividadDTO {
    private List<RegistroMensualDTO> registros;
    private Long totalDonaciones;
    private Long totalOrganizaciones;
}
