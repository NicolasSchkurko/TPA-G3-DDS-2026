package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.*;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.Metricas;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.*;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorActividad;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorPerfiles;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorRanking;
import org.springframework.stereotype.Service;

@Service
public class PerfilService {
    private final GestorPerfiles perfiles;
    private final GestorRanking rankings;
    private final GestorActividad actividad;

    public PerfilService(GestorPerfiles perfiles,
                         GestorRanking rankings,
                         GestorActividad actividad) {
        this.perfiles = perfiles;
        this.rankings = rankings;
        this.actividad = actividad;
    }

    /**
     * Busca el perfil usando el identificador del usuario (el identificador
     * que llega desde donaciones-service), no el identificador interno del
     * perfil.
     */
    public PerfilDTO buscarPorIdUsuario(UUID idUsuario){
        Perfil p = perfiles.obtenerPerfil(idUsuario);
        if (p == null) {
            return null;
        }

        return new PerfilDTO(
                p.getNombreUsuario(),
                p.getCategoriaActual() == null ? null : p.getCategoriaActual().getNombre(),
                p.getInsignias() == null ? List.of() : p.getInsignias().stream().map(Insignia::getNombre).toList(),
                p.getMisionActual() == null ? null : p.getMisionActual().getNombreMision(),
                p.getPosicionRanking() == null ? null : p.getPosicionRanking().getPuesto());
    }

    /**
     * Alias de compatibilidad para los consumidores que ya utilizaban este
     * método con el nombre anterior.
     */
    public PerfilDTO buscarPorId(UUID idUsuario){
        return buscarPorIdUsuario(idUsuario);
    }

    public MetricasHistoricasDTO obtenerMetricasDonante(UUID idUsuario, UUID idPerfil){
        //% de variacion de donaciones x mes
        List<Metricas> metricasHistoricas = actividad.comparacionHistorica(idPerfil);

        return new MetricasHistoricasDTO(
                metricasHistoricas.stream()
                .map(this::convertirMetricaADTO).toList()
        );
    }

    public ActividadDTO obtenerEvolucionHistorica(UUID idUsuario, UUID idPerfil){
        Integer donacionesTotales = actividad.donacionesTotales(idPerfil);
        Integer cantidadOrgsAyudadas = actividad.cantidadOrganizacionesAyudadas(idPerfil);
        //obtener cantidad de donaciones y organizaciones ayudadas x mes
        Map<YearMonth, Integer> donacionesXMes = actividad.actividadPerfilDonaciones(idPerfil);
        Map<YearMonth, Integer> orgsAyudadasXMes = actividad.actividadPerfilOrganizaciones(idPerfil);

        TreeSet<YearMonth> meses = new TreeSet<>(donacionesXMes.keySet());
        meses.addAll(orgsAyudadasXMes.keySet());

        List<RegistroMensualDTO> actividadPerfil = meses.stream()
                .map(mes -> new RegistroMensualDTO(
                            mes,
                            donacionesXMes.getOrDefault(mes, 0),
                            orgsAyudadasXMes.getOrDefault(mes, 0)
                        )
                )
                .toList();



        return new ActividadDTO(
                actividadPerfil,
                donacionesTotales,
                cantidadOrgsAyudadas
        );
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

    public MetricaDTO convertirMetricaADTO(Metricas metrica){
        return new MetricaDTO(
                metrica.getInicio(),
                metrica.getFin(),
                metrica.getVariacionPorcentual());
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
