package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.ActividadMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.MetricasActividad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.*;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorPerfiles;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorRanking;
import org.springframework.stereotype.Service;

@Service
public class PerfilService {
    private final GestorPerfiles perfiles;
    private final GestorRanking rankings;

    public PerfilService(GestorPerfiles perfiles,
                         GestorRanking rankings) {
        this.perfiles = perfiles;
        this.rankings = rankings;
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

    public RankingMesDTO obtenerRanking(UUID idRanking) {
        RankingMensual rank = rankings.obtenerRanking(idRanking);

        return new RankingMesDTO(
                rank.getPosiciones().stream()
                                .map(this::convertirRankingADTO).toList(),
                rank.getPeriodo());
    }

    public RankingMesDTO obtenerTop3Ranking(UUID idRanking) {
        RankingMensual top3 = rankings.obtenerTop3(idRanking);

        return new RankingMesDTO(
                top3.getPosiciones().stream()
                        .map(this::convertirRankingADTO).toList(),
                top3.getPeriodo());
    }

    public MisionPerfilDTO obtenerMisionPorID(UUID idUsuario) {
        Mision mision = perfiles.obtenerMisionPerfil(idUsuario);

        return convertirMisionPerfilADTO(mision);
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

    public MisionPerfilDTO convertirMisionPerfilADTO(Mision mision) {
        return new MisionPerfilDTO(
                        mision.getNombreMision(),
                        mision.getDescripcion(),
                        mision.getInsigniaObjetivo().getNombre()
                );
    }

    public ListaInsigniasDTO obtenerInsigniasPorID(UUID idUsuario) {
        List<Insignia> insignias = perfiles.obtenerInsigniasPerfil(idUsuario);

        List<InsigniaDTO> dto = insignias.stream().map(this::convertirInsigniaADTO).toList();

        return new ListaInsigniasDTO(dto);
    }

    public InsigniaDTO convertirInsigniaADTO(Insignia insignia) {
        return new InsigniaDTO( insignia.getNombre(),
                                insignia.getDescripcion(),
                                insignia.getUrlImagen(),
                                insignia.getFechaObtencion()
        );
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