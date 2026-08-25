package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import java.time.YearMonth;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MetricaDTO {
    private YearMonth inicio;
    private YearMonth fin;
    private Double variacionPorcentualDonaciones;

    public MetricaDTO(YearMonth inicio, YearMonth fin,
                                 Double variacionPorcentualDonaciones){
        this.inicio = inicio;
        this.fin = fin;
        this.variacionPorcentualDonaciones = variacionPorcentualDonaciones;
    }
}
