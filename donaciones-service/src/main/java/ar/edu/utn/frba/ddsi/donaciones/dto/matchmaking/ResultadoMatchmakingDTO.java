package ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionResumenDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ResultadoMatchmakingDTO {
    private DonacionResumenDTO donacion;
    private List<PropuestaAsignacionDTO> propuestasOrdenadas;
    private boolean huboCoincidenciaTotal;

    public static ResultadoMatchmakingDTO from(ResultadoMatchmaking resultado) {
        if (resultado == null) return null;
        ResultadoMatchmakingDTO dto = new ResultadoMatchmakingDTO();
        dto.setDonacion(DonacionResumenDTO.from(resultado.getDonacion()));
        if (resultado.getPropuestasOrdenadas() != null) {
            dto.setPropuestasOrdenadas(resultado.getPropuestasOrdenadas().stream().map(PropuestaAsignacionDTO::from).collect(Collectors.toList()));
        }
        dto.setHuboCoincidenciaTotal(resultado.isHuboCoincidenciaTotal());
        return dto;
    }
}