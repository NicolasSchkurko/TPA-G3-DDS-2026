package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.*;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.*;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorCategoria;

import java.util.*;

import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioRankings;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final RepositorioPerfiles repoPerfiles;
    private final RepositorioDonaciones repoDonaciones;
    private final GestorPerfiles perfiles;
    private final GestorCategoria categorias;
    private final RepositorioRankings repoRankings;
    private final DonacionClient donacionClient;

    public UserService(RepositorioPerfiles repositorio,
                       RepositorioDonaciones repoDonaciones,
                       GestorPerfiles perfiles,
                       GestorCategoria categorias,
                       DonacionClient donacionClient,
                       RepositorioRankings rankings) {
        this.repoPerfiles = repositorio;
        this.repoDonaciones = repoDonaciones;
        this.perfiles = perfiles;
        this.categorias = categorias;
        this.donacionClient = donacionClient;
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

        Categoria categoriaAnterior = p.getCategoriaActual();
        Mision misionAnterior = p.getProgresoMisionActual() == null
                ? null
                : p.getProgresoMisionActual().getMision();

        Boolean misionCompletada = perfiles.progresarPerfil(p, donacion);

        if (Boolean.TRUE.equals(misionCompletada)
                && categoriaAnterior != null
                && misionAnterior != null) {
            MedioContacto contacto = donacionClient.obtenerContactoPersona(idUsuario);

            if (categoriaAnterior.esUltimaMision(misionAnterior)) {
                Categoria siguienteCategoria = categorias.obtenerCategoriaSiguiente(categoriaAnterior);
                if (siguienteCategoria != null) {
                    p.cambiarCategoria(siguienteCategoria, categoriaAnterior, misionAnterior, contacto);
                }
            } else {
                Mision siguienteMision = categoriaAnterior.siguienteMision(misionAnterior);
                if (siguienteMision != null) {
                    p.cambiarMision(siguienteMision, misionAnterior, contacto);
                }
            }
        }

        repoPerfiles.save(p);
        repoDonaciones.save(donacion);

        return misionCompletada;
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
