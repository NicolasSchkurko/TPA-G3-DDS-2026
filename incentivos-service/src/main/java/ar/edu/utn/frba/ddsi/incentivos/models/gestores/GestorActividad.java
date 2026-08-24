package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.ActividadMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.HistorialActividad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.Metricas;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioActividades;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public record GestorActividad(RepositorioActividades repositorio) {
    public void guardarActividad(HistorialActividad actividad){
        repositorio.guardar(actividad);
    }

    /** Registra la donacion en el historial del perfil, creandolo si es necesario. */
    public void guardarDonacion(UUID idPerfil, ImpactoDonacion donacion) {
        if (idPerfil == null || donacion == null) {
            return;
        }

        HistorialActividad actividad = repositorio.buscarPorIdPerfil(idPerfil);
        if (actividad == null) {
            actividad = new HistorialActividad(idPerfil, new ArrayList<>());
        }
        actividad.agregarDonacion(donacion);
        guardarActividad(actividad);
    }

    public Integer donacionesTotales(UUID idPerfil){
        HistorialActividad actividad = repositorio.buscarPorIdPerfil(idPerfil);
        return actividad.cantidadDonacionesTotales();
    }

    public Integer cantidadOrganizacionesAyudadas(UUID idPerfil){
        HistorialActividad actividad = repositorio.buscarPorIdPerfil(idPerfil);
        return actividad.cantidadEntidadesBeneficiadas();
    }

    /** Cantidad de donaciones agrupada por mes, ordenada cronologicamente. */
    public Map<YearMonth, Integer> actividadPerfilDonaciones(UUID idPerfil){
        HistorialActividad actividad = repositorio.buscarPorIdPerfil(idPerfil);
        return actividad.getActividadPorMes().stream()
                .collect(Collectors.toMap(
                        ActividadMensual::getPeriodo,
                        actividadMensual -> actividadMensual.getDonacionesEnMes().size(),
                        Integer::sum,
                        LinkedHashMap::new));
    }

    /** Cantidad de organizaciones ayudadas agrupada por mes, ordenada cronologicamente. */
    public Map<YearMonth, Integer> actividadPerfilOrganizaciones(UUID idPerfil){
        HistorialActividad actividad = repositorio.buscarPorIdPerfil(idPerfil);
        return actividad.getActividadPorMes().stream()
                .collect(Collectors.toMap(
                        ActividadMensual::getPeriodo,
                        actividadMensual -> actividadMensual.entidadesBeneficiadas().size(),
                        Integer::sum,
                        LinkedHashMap::new));
    }

    public Metricas comparacionDeterminada(UUID idPerfil, YearMonth inicio, YearMonth fin){
        HistorialActividad actividad = repositorio.buscarPorIdPerfil(idPerfil);
        return actividad.calcularMetricasDeterminada(ImpactoDonacion::getCantidadBienes, inicio, fin);
    }

    public List<Metricas> comparacionHistorica(UUID idPerfil){
        HistorialActividad actividad = repositorio.buscarPorIdPerfil(idPerfil);
        return actividad.calcularMetricasMensuales(ImpactoDonacion::getCantidadBienes);
    }
}
