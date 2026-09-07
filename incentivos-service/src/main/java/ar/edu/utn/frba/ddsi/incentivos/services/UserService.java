package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.*;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ActividadMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.*;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorPerfiles;

import java.time.YearMonth;
import java.util.*;

import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioRankings;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioActividad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.MetricaActividad;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final RepositorioPerfiles repoPerfiles;
    private final GestorPerfiles perfiles;
    private final RepositorioRankings repoRankings;
    private final RepositorioActividad repoActividadMensual;

    public UserService(RepositorioPerfiles repositorio,
                       GestorPerfiles perfiles,
                       RepositorioRankings rankings,
                       RepositorioActividad repoActividadMensual) {
        this.repoPerfiles = repositorio;
        this.perfiles = perfiles;
        this.repoRankings = rankings;
        this.repoActividadMensual = repoActividadMensual;
    }

    public ActividadDTO obtenerEvolucionHistorica(UUID idUsuario) {
        List<ActividadMensual> actividades =
                repoActividadMensual
                        .findByPerfilIdUsuarioOrderByPeriodoAsc(idUsuario);

        List<RegistroMensualDTO> registros = actividades.stream()
                .map(a -> new RegistroMensualDTO(
                        a.getPeriodo(),
                        a.getCantidadDonaciones(),
                        a.getCantidadOrganizaciones()
                ))
                .toList();

        int totalDonaciones = repoActividadMensual
                .totalDonaciones(idUsuario)
                .intValue();

        int totalOrganizaciones = repoActividadMensual
                .totalOrganizaciones(idUsuario)
                .intValue();

        return new ActividadDTO(
                registros,
                totalDonaciones,
                totalOrganizaciones
        );
    }

    /**
     * Calcula la variación usando solamente los resúmenes mensuales guardados,
     * no leyendo todas las donaciones individuales.
     */
    public List<MetricaDTO> obtenerMetricasHistoricas(UUID idUsuario) {
        return repoActividadMensual
                .obtenerMetricasDesdeActividad(idUsuario)
                .stream()
                .map(this::convertirMetricaADTO)
                .toList();
    }

    public Optional<MetricaDTO> obtenerMetricaPeriodoActual(UUID idUsuario) {
        YearMonth periodoActual = YearMonth.now();

        return repoActividadMensual
                .obtenerMetricaPeriodoActual(idUsuario, periodoActual)
                .map(this::convertirMetricaADTO);
    }

    public List<InsigniaDTO> obtenerInsigniasPorIdUsuario(UUID idUsuario) {
        // distinguir entre perfil inexistente y perfil sin insignias.
        repoPerfiles.findByIdUsuario(idUsuario)
                .orElseThrow(InexistenteException::new);

        return repoPerfiles.obtenerInsigniasPorIdUsuario(idUsuario)
                .stream()
                .map(this::convertirInsigniaADTO)
                .toList();
    }

    public MisionPerfilDTO obtenerMisionPorIdUsuario(UUID idUsuario) {
        repoPerfiles.findByIdUsuario(idUsuario)
                .orElseThrow(InexistenteException::new);

        Mision mision = repoPerfiles.obtenerMisionPorIdUsuario(idUsuario)
                .orElseThrow();
        return convertirMisionPerfilADTO(mision);
    }

    @Transactional
    public Boolean actualizarPerfil(UUID idUsuario, ImpactoDonacionDTO dto) {
        if (idUsuario == null) {
            return null;
        }

        ImpactoDonacion donacion = this.convertirDTO(idUsuario, dto);
        Perfil p = repoPerfiles.findByIdUsuario(idUsuario)
                .orElseThrow(InexistenteException::new);

        ActividadMensual actividadMensual = repoActividadMensual
                .findByPerfilAndPeriodo(p, YearMonth.from(donacion.getFechaEntrega()))
                .orElseGet(() -> new ActividadMensual(
                        p,
                        YearMonth.from(donacion.getFechaEntrega())
                ));

        actividadMensual.registrarDonacion(
                donacion.getCantidadBienes(),
                donacion.getEntidadBeneficiaria()
        );
        repoActividadMensual.save(actividadMensual);

        return perfiles.progresarPerfil(p, donacion);
    }

    public RankingMesDTO obtenerRanking(UUID idRanking) {
        RankingMensual rank = repoRankings.findById(idRanking)
                .orElseThrow(InexistenteException::new);

        return new RankingMesDTO(
                rank.getPosiciones().stream()
                        .map(this::convertirRankingADTO).toList(),
                rank.getPeriodo());
    }

    public RankingMesDTO obtenerTop3Ranking(UUID idRanking) {
        RankingMensual rank = repoRankings.findById(idRanking)
                .orElseThrow(InexistenteException::new);

        return new RankingMesDTO(
                rank.getPosiciones().stream()
                        .limit(3)
                        .map(this::convertirRankingADTO).toList(),
                rank.getPeriodo());
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

    private MetricaDTO convertirMetricaADTO(MetricaActividad metrica) {
        return new MetricaDTO(
                metrica.getInicio(),
                metrica.getFin(),
                metrica.getVariacion()
        );
    }

    public MisionPerfilDTO convertirMisionPerfilADTO(Mision mision) {
        return new MisionPerfilDTO(
                        mision.getNombreMision(),
                        mision.getDescripcion(),
                        mision.getInsigniaObjetivo().getNombre()
                );
    }

    public InsigniaDTO convertirInsigniaADTO(Insignia insignia) {
        return new InsigniaDTO( insignia.getNombre(),
                                insignia.getDescripcion(),
                                insignia.getUrlImagen()
        );
    }

    public RankingDTO convertirRankingADTO(Ranking ranking) {
        RankingDTO dto = new RankingDTO();
        dto.setPosicionRanking(ranking.getPuesto());
        dto.setNombreUsuario(ranking.getNombreUsuario());
        dto.setCantidadMisionesCompletas(
                ranking.getMisionesCumplidas().intValue()
        );
        return dto;
    }
}
