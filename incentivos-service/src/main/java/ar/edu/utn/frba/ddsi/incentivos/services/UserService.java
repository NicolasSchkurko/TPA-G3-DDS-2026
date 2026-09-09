package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.*;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.*;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorPerfiles;

import java.util.*;

import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioRankings;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final RepositorioPerfiles repoPerfiles;
    private final GestorPerfiles perfiles;
    private final RepositorioRankings repoRankings;

    public UserService(RepositorioPerfiles repositorio,
                       GestorPerfiles perfiles,
                       RepositorioRankings rankings) {
        this.repoPerfiles = repositorio;
        this.perfiles = perfiles;
        this.repoRankings = rankings;
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
        return new RankingDTO(
                ranking.getNombreUsuario(),
                ranking.getPuesto(),
                ranking.getMisionesCumplidas()
        );
    }
}
