package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import lombok.Getter;
import lombok.Setter;

import java.time.temporal.ChronoUnit;
//chrono me permitiria utilizar mas periodos de tiempo
//pero se tendria que cambiar de ImpactoDonacion y del servicio de donaciones
//por ser LocalDate, asi que sera una limitacion del servicio
//este chrono solo podra usar: minutos-horas-dias-semanas-meses-años

@Getter
@Setter
public class ReglaConstancia {
    private Integer cantidad;
    private ChronoUnit unidadTiempo;
}
