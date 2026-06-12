package ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ImpactoDonacion;
import lombok.Getter;
import lombok.Setter;
import ar.edu.utn.frba.ddsi.incentivos.dto.ActividadMensualDTO;

@Getter
@Setter
public class ActividadMensual {
    private YearMonth periodo;
    private Integer cantidadDonaciones;
    private Integer organizacionesAyudadas;

    public ActividadMensual(YearMonth periodo, List<ImpactoDonacion> historial) {
        this.periodo = periodo;

        List<ImpactoDonacion> donacionesDelMes = historial.stream()
                .filter(d -> d.getFechaEntrega() != null && YearMonth.from(d.getFechaEntrega()).equals(periodo))
                .filter(d -> "ENTREGADA".equalsIgnoreCase(d.getEstado()))
                .toList();
        this.cantidadDonaciones = donacionesDelMes.size();

        this.organizacionesAyudadas = (int) donacionesDelMes.stream()
                .map(ImpactoDonacion::getEntidadBeneficiaria)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }
}
