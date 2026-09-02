package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.*;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PersonaService {
    private final GestorActividad gestorActividad;
    private final RepositorioPerfiles repositorioPerfiles;
    private final GestorPerfiles gestorPerfiles;
    private final GestorCategoria gestorCategorias;

    public PersonaService(GestorActividad gestorImpacto,
                          RepositorioPerfiles repositorio,
                          GestorPerfiles gestorPerfiles,
                          GestorCategoria gestorCategorias) {
        this.repositorioPerfiles = repositorio;
        this.gestorPerfiles = gestorPerfiles;
        this.gestorCategorias = gestorCategorias;
        this.gestorActividad = gestorImpacto;
    }

    public PerfilDTO crearPerfil(PerfilDonanteDTO dto) {
        Perfil nuevo = new Perfil(dto.getIdUsuario(), dto.getNombreUsuario(), dto.getRole());

        boolean rolUser = dto.getRole().toUpperCase(Locale.ROOT).equals("USER");
        Categoria categoriaBase = rolUser ?
                gestorCategorias.categoriaCorrespondiente(1)
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
                nuevo.getPosicionRanking() == null ? null : nuevo.getPosicionRanking().getPuesto()
        );
    }

    public PerfilDTO actualizarPerfil(UUID idUsuario, ImpactoDonacionDTO dto) {
        if (idUsuario == null) {
            return null;
        }

        ImpactoDonacion donacion = this.convertirDTO(idUsuario, dto);
        Perfil p = gestorPerfiles.progresarPerfil(idUsuario, donacion);
        if (p == null) return null;

        gestorActividad.guardarDonacion(p.getIdPerfil(), donacion);

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
}
