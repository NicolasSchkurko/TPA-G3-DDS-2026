package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.*;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.Metricas;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
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

    public PerfilDTO actualizarPerfil(UUID idUsuario, ImpactoDonacionDTO dto) {
        if (idUsuario == null) {
            return null;
        }

        ImpactoDonacion donacion = this.convertirDTO(idUsuario, dto);
        Perfil p = perfiles.progresarPerfil(idUsuario, donacion);
        if (p == null) return null;

        actividad.guardarDonacion(p.getIdPerfil(), donacion);

        return new PerfilDTO(
                p.getNombreUsuario(),
                p.getCategoriaActual().getNombre(),
                p.getInsignias().stream().map(Insignia::getNombre).toList(),
                p.getMisionActual().getNombreMision(),
                p.getPosicionRanking().getPuesto(),
                p.getRole() == null ? null : p.getRole().name()
        );
    }

    public ImpactoDonacion convertirDTO(UUID id, ImpactoDonacionDTO donacion){
        return new ImpactoDonacion(donacion.getEntidadBeneficiaria(),
                donacion.getCantidadBienes(),
                donacion.getFechaEntrega(),
                donacion.getCategoria(),
                donacion.getSubCategoria(),
                donacion.getEstado(),
                id);
    }

    public List<MetricaDTO> obtenerMetricasDonante(UUID idUsuario, UUID idPerfil){
        //% de variacion de donaciones x mes
        List<Metricas> metricasHistoricas = actividad.comparacionHistorica(idPerfil);

        return metricasHistoricas.stream()
                .map(this::convertirMetricaADTO)
                .toList();
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

    public List<InsigniaDTO> obtenerInsigniasPorID(UUID idUsuario) {
        List<Insignia> insignias = perfiles.obtenerInsigniasPerfil(idUsuario);

        return insignias.stream().map(this::convertirInsigniaADTO).toList();
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
