package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.*;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.HistorialActividad;
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
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioActividades;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioRankings;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final RepositorioPerfiles repoPerfiles;
    private final GestorPerfiles perfiles;
    private final GestorActividad actividad;
    private final RepositorioActividades repoActividades;
    private final RepositorioRankings repoRankings;

    public UserService(RepositorioPerfiles repositorio,
                       GestorPerfiles perfiles,
                       RepositorioRankings rankings,
                       GestorActividad actividad, RepositorioActividades repoActividades) {
        this.repoPerfiles = repositorio;
        this.perfiles = perfiles;
        this.repoRankings = rankings;
        this.actividad = actividad;
        this.repoActividades= repoActividades;
    }

    public List<InsigniaDTO> obtenerInsigniasPorIdUsuario(UUID idUsuario) {
        List<Insignia> insignias = repoPerfiles.buscarPorIDUsuario(idUsuario).getInsignias();
        return insignias.stream().map(this::convertirInsigniaADTO).toList();
    }

    public MisionPerfilDTO obtenerMisionPorIdUsuario(UUID idUsuario) {
        Mision mision = repoPerfiles.buscarPorIDUsuario(idUsuario).getProgresoMisionActual().getMision();
        return convertirMisionPerfilADTO(mision);
    }

    public List<MetricaDTO> obtenerMetricasDonante(UUID idUsuario, UUID idPerfil){
        //% de variacion de donaciones x mes
        HistorialActividad actividad = repoActividades.buscarPorIdPerfil(idPerfil);

        return actividad.calcularMetricasMensuales(ImpactoDonacion::getCantidadBienes).stream()
                .map(this::convertirMetricaADTO)
                .toList();
    }

    public ActividadDTO obtenerEvolucionHistorica(UUID idUsuario, UUID idPerfil){
        HistorialActividad historial = repoActividades.buscarPorIdPerfil(idPerfil);
        Integer donacionesTotales = historial.cantidadDonacionesTotales();
        Integer cantidadOrgsAyudadas = historial.cantidadEntidadesBeneficiadas();
        //obtener cantidad de donaciones y organizaciones ayudadas x mes
        Map<YearMonth, Integer> donacionesXMes = actividad.actividadPerfilDonaciones(historial);
        Map<YearMonth, Integer> orgsAyudadasXMes = actividad.actividadPerfilOrganizaciones(historial);

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
        RankingMensual rank = repoRankings.buscarPorId(idRanking);

        return new RankingMesDTO(
                rank.getPosiciones().stream()
                        .map(this::convertirRankingADTO).toList(),
                rank.getPeriodo());
    }

    public RankingMesDTO obtenerTop3Ranking(UUID idRanking) {
        RankingMensual rank = repoRankings.buscarPorId(idRanking);

        List<Ranking> top3 =  rank.getPosiciones().stream()
                .limit(3) // Nos quedamos solo con los 3 primeros elementos de la lista ya ordenada
                .toList();

        rank.setPosiciones(top3);

        return new RankingMesDTO(
                rank.getPosiciones().stream()
                        .map(this::convertirRankingADTO).toList(),
                rank.getPeriodo());
    }

    public PerfilDTO actualizarPerfil(UUID idUsuario, ImpactoDonacionDTO dto) {
        if (idUsuario == null) {
            return null;
        }

        ImpactoDonacion donacion = this.convertirDTO(idUsuario, dto);
        Perfil p = perfiles.progresarPerfil(idUsuario, donacion);
        if (p == null) return null;

        repoActividades.guardarDonacion(p.getIdPerfil(), donacion);

        return new PerfilDTO(
                p.getNombreUsuario(),
                p.getCategoriaActual().getNombre(),
                p.getInsignias().stream().map(Insignia::getNombre).toList(),
                p.getMisionActual().getNombreMision(),
                p.getPosicionRanking().getPuesto()
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
