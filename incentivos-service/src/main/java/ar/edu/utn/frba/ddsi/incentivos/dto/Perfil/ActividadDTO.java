package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActividadDTO {
    //retorna la actividad del perfil desde su inicio hasta actualidad
    private List<RegistroMensualDTO> actividadPerfil;
    private Integer donacionesTotales;
    private Integer organizacionesAyudadasTotales;

    public ActividadDTO(List<RegistroMensualDTO> actividad,
                        Integer donacionesTotales,
                        Integer organizacionesAyudadasTotales){
        this.actividadPerfil = actividad;
        this.donacionesTotales = donacionesTotales;
        this.organizacionesAyudadasTotales = organizacionesAyudadasTotales;
    }
}
