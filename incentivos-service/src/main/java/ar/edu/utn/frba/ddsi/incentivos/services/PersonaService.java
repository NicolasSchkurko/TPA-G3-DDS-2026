package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.*;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PersonaService {
    private final RepositorioPerfiles repositorioPerfiles;
    private final RepositorioCategorias repositorioCategorias;

    public PersonaService(RepositorioPerfiles repositorio,
                          RepositorioCategorias repositorioCategorias) {
        this.repositorioPerfiles = repositorio;
        this.repositorioCategorias = repositorioCategorias;
    }

    public PerfilDTO crearPerfil(PerfilDonanteDTO dto) {
        Perfil nuevo = new Perfil(dto.getIdUsuario(), dto.getNombreUsuario(), dto.getRole());

        boolean rolUser = dto.getRole().toUpperCase(Locale.ROOT).equals("USER");
        //todo: deberian comunicarse por evento o lo dejo asi?
        Categoria categoriaBase = rolUser ?
                repositorioCategorias.buscarPorPosicionSecuencia(1)
                : null;
//        if (rolUser  && categoriaBase == null) {
//            throw new IllegalStateException("No existe una categoría base para inicializar el perfil");
//        }
        if (rolUser) {
            nuevo.setCategoriaActual(categoriaBase);
            nuevo.setMisionActual(categoriaBase.primeraMision());
        }

        if (repositorioPerfiles.buscarPorIDUsuario(nuevo.getIdUsuario()) != null) return null;
        repositorioPerfiles.agregarPerfil(nuevo);

        nuevo = repositorioPerfiles.buscarPorIDPerfil(nuevo.getIdPerfil());
//        if (p == null || (rolUser && (p.getCategoriaActual() == null || p.getMisionActual() == null))) {
//            throw new IllegalStateException("El perfil se creó sin categoría o misión iniciales");
//        }
        return new PerfilDTO(
                nuevo.getNombreUsuario(),
                nuevo.getCategoriaActual() == null ? null : nuevo.getCategoriaActual().getNombre(),
                nuevo.getInsignias() == null ? List.of() : nuevo.getInsignias().stream().map(Insignia::getNombre).toList(),
                nuevo.getMisionActual() == null ? null : nuevo.getMisionActual().getNombreMision(),
                nuevo.getPosicionRanking() == null ? null : nuevo.getPosicionRanking().getPuesto(),
                nuevo.getRole() == null ? null : nuevo.getRole().name()
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
