package ar.edu.utn.frba.ddsi.incentivos.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public class PerfilDonanteDTO {
    //los coloque en el orden que voy leyendo del tp, dsp se puede cambiar
    //datos visibles publicamente en el perfil del usuario
    private Integer totalDonaciones; //total historico de donaciones
    private List<ComparacionMensualDTO> evolucionMensual; //con cmp mensual hacemos el grafico de evolucion
    private Integer organizacionesAyudadas;
    private Integer posicionRanking; //posicion en ranking de donantes activos

    private String nombreUsuario;
    private String categoria;

    //alguna forma de mostrarle al usu su impacto en la plataforma
    private List<InsigniaObtenidaDTO> insigniasObtenidas;

    //visualizacion de su progreso en su mision actual
    private MisionDTO misionActual;
}
