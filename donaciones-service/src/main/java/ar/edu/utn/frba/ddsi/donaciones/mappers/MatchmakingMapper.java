package ar.edu.utn.frba.ddsi.donaciones.mappers;

import ar.edu.utn.frba.ddsi.donaciones.dto.*;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.AsignadorDonaciones.ResultadoMatchmaking;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MatchmakingMapper {

    private final DonacionMapper donacionMapper;

    public MatchmakingMapper(DonacionMapper donacionMapper) {
        this.donacionMapper = donacionMapper;
    }

    public ResultadoMatchmakingDTO ResultadoToDTO(ResultadoMatchmaking resultado) {
        if (resultado == null) return null;

        ResultadoMatchmakingDTO dto = new ResultadoMatchmakingDTO();

        // Donación
        dto.setDonacion(
                donacionMapper.toDTO(resultado.getDonacion())
        );

        // Propuestas
        List<PropuestaAsignacionDTO> propuestas = resultado
                .getPropuestasOrdenadas()
                .stream()
                .map(this::PropuestaToDTO)
                .toList();

        dto.setPropuestasOrdenadas(propuestas);

        dto.setHuboCoincidenciaTotal(resultado.isHuboCoincidenciaTotal());

        return dto;
    }

    public PropuestaAsignacionDTO PropuestaToDTO(PropuestaAsignacion propuesta) {
        if (propuesta == null) return null;

        PropuestaAsignacionDTO dto = new PropuestaAsignacionDTO();

        dto.setEntidad(
                donacionMapper.toDTO(propuesta.getEntidad())
        );

        dto.setNecesidad(
                donacionMapper.toDTO(propuesta.getNecesidad())
        );

        dto.setAlgoritmo(propuesta.getAlgoritmo());
        dto.setPosicion(propuesta.getPosicion());
        dto.setScore(propuesta.getScore());

        return dto;
    }
}
