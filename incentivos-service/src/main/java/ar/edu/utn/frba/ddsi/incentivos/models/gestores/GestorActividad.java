package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.ActividadMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.HistorialActividad;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioActividades;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public record GestorActividad(RepositorioActividades repositorio) {
    /** Cantidad de donaciones agrupada por mes, ordenada cronologicamente. */
    public Map<YearMonth, Integer> actividadPerfilDonaciones(HistorialActividad actividad){
        return actividad.getActividadPorMes().stream()
                .collect(Collectors.toMap(
                        ActividadMensual::getPeriodo,
                        actividadMensual -> actividadMensual.getDonacionesEnMes().size(),
                        Integer::sum,
                        LinkedHashMap::new));
    }

    /** Cantidad de organizaciones ayudadas agrupada por mes, ordenada cronologicamente. */
    public Map<YearMonth, Integer> actividadPerfilOrganizaciones(HistorialActividad actividad){
        return actividad.getActividadPorMes().stream()
                .collect(Collectors.toMap(
                        ActividadMensual::getPeriodo,
                        actividadMensual -> actividadMensual.entidadesBeneficiadas().size(),
                        Integer::sum,
                        LinkedHashMap::new));
    }
}
