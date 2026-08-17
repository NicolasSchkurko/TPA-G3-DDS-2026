package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.MedioContactoDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PersonaService {
    private final GestorImpacto gestorImpacto;
    private final GestorN8N gestorN8N;
    private final GestorPerfiles gestorPerfiles;
    private final GestorCategoria gestorCategorias;
    private final GestorNotificaciones gestorNotificaciones;

    public PersonaService(GestorImpacto gestorImpacto,
                          GestorN8N gestorN8N,
                          GestorPerfiles gestorPerfiles,
                          GestorCategoria gestorCategorias,
                          GestorNotificaciones gestorNotificaciones) {
        this.gestorNotificaciones = gestorNotificaciones;
        this.gestorPerfiles = gestorPerfiles;
        this.gestorCategorias = gestorCategorias;
        this.gestorImpacto = gestorImpacto;
        this.gestorN8N = gestorN8N;
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
                nuevo.getNombreUsuario(),
                nuevo.getCategoriaActual().getNombre(),
                nuevo.getInsignias().stream().map(Insignia::getNombre).toList(),
                nuevo.getMisionActual().getNombreMision(),
                nuevo.getPosicionRanking().getPuesto()
        );
    }

    public PerfilDTO actualizarPerfil(UUID idUsuario, ImpactoDonacionDTO dto) {
        if (idUsuario == null) {
            return null;
        }

        //llevare la lista de actualizaciones al gestorNotificaciones
        //para que se envie la notificacion correspondiente
        ImpactoDonacion donacion = this.convertirDTO(idUsuario, dto);
        List<Boolean> actualizaciones = gestorPerfiles.progresarPerfil(idUsuario, donacion);

        Perfil p = gestorPerfiles.conseguirPerfil(idUsuario);
        //[0] indica si se actualizo mision, [1] indica si hay que actualizar categoria
        if (actualizaciones != null && actualizaciones.get(1)) {
            Integer nivelActual = p.getCategoriaActual().getPosicionSecuencia();
            Categoria siguiente = gestorCategorias.categoriaCorrespondiente(nivelActual + 1);

            if (siguiente != null) {
                p.setCategoriaActual(siguiente);
                p.setMisionActual(siguiente.primeraMision());
            }
            p = gestorPerfiles.actualizarPerfil(p);
        }

        gestorImpacto.actualizarDonaciones(donacion);

        MedioContactoDTO contactoDTO = gestorImpacto.obtenerContacto(p.getIdUsuario());
        gestorNotificaciones.enviarNotificaciones(actualizaciones, p, contactoDTO);

        gestorN8N.enviarNotificaciones(actualizaciones, p);

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