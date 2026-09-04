package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.CategoriaBaseInexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.PerfilExistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PerfilService {
    private final RepositorioPerfiles repositorioPerfiles;
    private final RepositorioCategorias repositorioCategorias;

    public PerfilService(RepositorioPerfiles repositorio,
                         RepositorioCategorias repositorioCategorias) {
        this.repositorioPerfiles = repositorio;
        this.repositorioCategorias = repositorioCategorias;
    }

    public PerfilDTO crearPerfil(PerfilDonanteDTO dto) {
        Perfil nuevo = new Perfil(dto.getIdUsuario(), dto.getNombreUsuario());

        Categoria categoriaBase = repositorioCategorias
                .buscarPorPosicionSecuencia(1)
                .orElseThrow(() -> new CategoriaBaseInexistenteException(
                        "No existe la categoría base configurada"));

        nuevo.setCategoriaActual(categoriaBase);
        nuevo.setProgresoMisionActual(new ProgresoMision(
                    categoriaBase.primeraMision()
                )
        );

        if (repositorioPerfiles.existsByIdUsuario(nuevo.getIdUsuario())) {
            throw new PerfilExistenteException(nuevo.getIdUsuario());
        }

        nuevo = repositorioPerfiles.save(nuevo);

        return new PerfilDTO(
                nuevo.getNombreUsuario(),
                nuevo.getCategoriaActual().getNombre(),
                nuevo.getInsignias().stream().map(Insignia::getNombre).toList(),
                nuevo.getProgresoMisionActual().getMision().getNombreMision(),
                nuevo.getPosicionRanking().getPuesto()
        );
    }

    public PerfilDTO buscarPorIdUsuario(UUID idUsuario){
        Perfil p = repositorioPerfiles.buscarPorIDUsuario(idUsuario);
        if (p == null) {
            return null;
        }

        return new PerfilDTO(
                p.getNombreUsuario(),
                p.getCategoriaActual() == null ? null : p.getCategoriaActual().getNombre(),
                p.getInsignias() == null ? List.of() : p.getInsignias().stream().map(Insignia::getNombre).toList(),
                p.getMisionActual() == null ? null : p.getMisionActual().getNombreMision(),
                p.getPosicionRanking() == null ? null : p.getPosicionRanking().getPuesto(),
                p.getRole() == null ? null : p.getRole().name());
    }
}
