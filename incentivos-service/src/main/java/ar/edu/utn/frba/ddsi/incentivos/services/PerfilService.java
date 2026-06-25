package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.ActividadMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.MetricasActividad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.*;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioRankings;
import org.springframework.stereotype.Service;

@Service
public class PerfilService {
    private final RepositorioDonaciones repositorioDonaciones;
    private final RepositorioPerfiles repositorioPerfiles;
    private final RepositorioRankings repositorioRankings;

    public PerfilService(RepositorioDonaciones repositorio,
                         RepositorioPerfiles perfiles,
                         RepositorioRankings rankings) {
        this.repositorioDonaciones = repositorio;
        this.repositorioPerfiles = perfiles;
        this.repositorioRankings = rankings;
    }

    public void verificarProgresos(){
        repositorioPerfiles.listarTodos()
                .forEach(Perfil::verificarProgresoMision);
    }

    public MetricasActividad obtenerMetricasDonante(UUID idUsuario){
        List<ImpactoDonacion> historial = repositorioDonaciones.buscarDonacionesPorIDUsuario(idUsuario);

        YearMonth mesActual = YearMonth.now();
        YearMonth mesAnterior = mesActual.minusMonths(1);

        ActividadMensual actividadActual = new ActividadMensual(mesActual, historial);
        ActividadMensual actividadAnterior = new ActividadMensual(mesAnterior, historial);

        return new MetricasActividad(actividadActual, actividadAnterior);
    }

    public List<ActividadMensual> obtenerEvolucionHistorica(UUID idUsuario){
        List<ImpactoDonacion> historial = repositorioDonaciones.buscarDonacionesPorIDUsuario(idUsuario);

        return historial.stream()
                .filter(d -> d.getFechaEntrega() != null && "ENTREGADA".equalsIgnoreCase(d.getEstado()))
                .map(d -> YearMonth.from(d.getFechaEntrega()))
                .distinct()
                .sorted() // Ordenados cronológicamente
                .map(periodo -> new ActividadMensual(periodo, historial))
                .toList();
    }

    public RankingMensual obtenerRankingDelMes(YearMonth periodo) {
        return repositorioRankings.buscarPorPeriodo(periodo);
    }

    public List<Ranking> obtenerTop3DelMes(YearMonth periodo) {
        RankingMensual ranking = repositorioRankings.buscarPorPeriodo(periodo);
        if (ranking == null || ranking.getPosiciones() == null) {
            return List.of();
        }

        return ranking.getPosiciones().stream()
                .limit(3) // Nos quedamos solo con los 3 primeros elementos de la lista ya ordenada
                .toList();
    }

    public Mision obtenerMisionPorID(UUID idUsuario) {
        Perfil perfil = repositorioPerfiles.buscarPorIDUsuario(idUsuario);

        return perfil.getMisionActual();
    }

    public MetricasActividadDTO convertirMetricaADTO(MetricasActividad metrica){
        return new MetricasActividadDTO(metrica.getPeriodo(),
                metrica.getVariacionPorcentualDonaciones(),
                metrica.getVariacionPorcentualOrganizaciones());
    }

    public ActividadMensualDTO convertirActividadADTO(ActividadMensual actividad){
        return new ActividadMensualDTO( actividad.getPeriodo(),
                actividad.getCantidadDonaciones(),
                actividad.getOrganizacionesAyudadas());
    }

    public MisionDTO convertirMisionADTO(Mision mision) {
        return new MisionDTO(mision.getNombreMision(),
                             mision.getProgresoActual(),
                             mision.getProgresoObjetivo());
    }

    public List<Insignia> obtenerInsigniasPorID(UUID idUsuario) {
        Perfil perfil = repositorioPerfiles.buscarPorIDUsuario(idUsuario);

        return perfil.getInsignias();
    }

    public InsigniaDTO convertirInsigniaADTO(Insignia insignia) {
        return new InsigniaDTO( insignia.getNombre(),
                                insignia.getDescripcion(),
                                insignia.getUrlImagen(),
                                insignia.getFechaObtencion());
    }

    public RankingDTO convertirRankingADTO(Ranking ranking) {
        if (ranking == null) return null;
        RankingDTO dto = new RankingDTO();
        dto.setPosicionRanking(ranking.getPosicionRanking().getPuesto());
        dto.setNombreUsuario(ranking.getNombreUsuario());
        dto.setCantidadMisionesCompletas(ranking.getPosicionRanking().getMisionesCumplidasEnPeriodo());
        return dto;
    }
}