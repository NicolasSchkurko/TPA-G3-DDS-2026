package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.*;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PersonaService {
    private final GestorActividad gestorActividad;
    private final GestorPerfiles gestorPerfiles;
    private final GestorCategoria gestorCategorias;

    public PersonaService(GestorActividad gestorImpacto,
                          GestorPerfiles gestorPerfiles,
                          GestorCategoria gestorCategorias) {
        this.gestorPerfiles = gestorPerfiles;
        this.gestorCategorias = gestorCategorias;
        this.gestorActividad = gestorImpacto;
    }

    public PerfilDTO crearPerfil(PerfilDonanteDTO dto) {
        Perfil nuevo = new Perfil(
                dto.getIdUsuario(),
                dto.getNombreUsuario()
        );

        Categoria categoriaBase = gestorCategorias.categoriaCorrespondiente(1); //creo q la categoria base va a estar en la posicion 1

        if (categoriaBase != null) {
            nuevo.setCategoriaActual(categoriaBase);
            nuevo.setMisionActual(categoriaBase.primeraMision());
        }

        Perfil p = gestorPerfiles.crearPerfil(nuevo);

        return new PerfilDTO(
                p.getNombreUsuario(),
                p.getCategoriaActual().getNombre(),
                p.getInsignias().stream().map(Insignia::getNombre).toList(),
                p.getMisionActual().getNombreMision(),
                p.getPosicionRanking().getPuesto()
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
