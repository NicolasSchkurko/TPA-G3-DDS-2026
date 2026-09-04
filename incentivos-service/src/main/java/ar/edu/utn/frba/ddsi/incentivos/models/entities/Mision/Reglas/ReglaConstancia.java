package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.temporal.ChronoUnit;
import java.util.UUID;
//chrono me permitiria utilizar mas periodos de tiempo
//pero se tendria que cambiar de ImpactoDonacion y del servicio de donaciones
//por ser LocalDate, asi que sera una limitacion del servicio
//este chrono solo podra usar: minutos-horas-dias-semanas-meses-años

@Getter
@Setter
@Entity
@NoArgsConstructor
public class ReglaConstancia {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Integer cantidad;

    @Enumerated(EnumType.STRING)
    private ChronoUnit unidadTiempo;

    public ReglaConstancia(
            Integer cantidad,
            ChronoUnit unidadTiempo
    ) {
        this.cantidad = cantidad;
        this.unidadTiempo = unidadTiempo;
    }
}
