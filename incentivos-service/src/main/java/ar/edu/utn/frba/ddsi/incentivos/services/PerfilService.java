package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;

import java.util.*;
import java.util.stream.Collectors;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.PerfilDuplicadoException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.PerfilInexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import org.springframework.stereotype.Service;

@Service
public class PerfilService {
    private final RepositorioPerfiles repositorioPerfiles = RepositorioPerfiles.getInstance();
    private final MetricasService metricasService;

    public PerfilService(MetricasService metricasService) {
        this.metricasService = metricasService;
    }

    public void crearPerfil(PerfilDonanteDTO dto) {
            if (dto.getIdUsuario() == null) {
                throw new DatosInvalidosException();
            }

            if (repositorioPerfiles.buscarPorIDUsuario(dto.getIdUsuario()) != null) {
                throw new PerfilDuplicadoException();
            }

            Perfil nuevo = new Perfil(dto.getIdUsuario(), dto.getNombreUsuario());
            repositorioPerfiles.agregarPerfil(nuevo);
        }

    public void actualizarPerfil(ImpactoDonacionDTO dto) {
        if (dto.getIdUsuario() == null) {
            throw new DatosInvalidosException();
        }

        ImpactoDonacion donacion = this.convertirDTO(dto);
        Perfil perfil = repositorioPerfiles.buscarPorIDUsuario(dto.getIdUsuario());

        if (repositorioPerfiles.buscarPorIDUsuario(dto.getIdUsuario()) == null) {
            throw new PerfilInexistenteException();
        }

    //guardar cosas a comparar para enviar notificacion
        if(perfil.progresarMision(donacion)){
            repositorioPerfiles.actualizar(perfil);
            //segun ascenso de categoria, gana insignia o lo q se quiera notificar
            NotificacionService.enviarNotificacion(
                new PerfilNotificacionDTO(
                        //crear notificacion
                ));
        }
    }

    public ImpactoDonacion convertirDTO(ImpactoDonacionDTO donacion){
        return new ImpactoDonacion(donacion.getEntidadBeneficiaria(),
                donacion.getCantidadBienes(),
                donacion.getFechaEntrega(),
                donacion.getCategoria(),
                donacion.getSubCategoria(),
                donacion.getEstado(),
                donacion.getIdUsuario());
    }

    public PerfilDonanteDTO buscarPerfilPorUUID(UUID id) {
        Perfil entidad = repositorioPerfiles.buscarPorIDUsuario(id);
        if (entidad == null) {
            return null;
        }
        return mapToDTO(entidad);
    }

    public List<PerfilNotificacionDTO> listarPerfiles() {
        List<Perfil> todas = repositorioPerfiles.getPerfiles();
        if (todas == null || todas.isEmpty()) {
            return new ArrayList<>();
        }

        return todas.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    //TODO arreglar para que sea por misiones completadas
    public void rankearPerfiles(){
        List<Perfil> perfiles = repositorioPerfiles.listarTodos();
        List<Perfil> perfilesRankeados = perfiles.stream()

        for (int i = 0; i < perfilesRankeados.size(); i++) {
            perfilesRankeados.get(i).setPosicionRanking(i + 1);
        }
    }

    private PerfilNotificacionDTO mapToDTO(Perfil perfil) {
        if (perfil == null) return null;

        PerfilNotificacionDTO dto = new PerfilNotificacionDTO();
        //TODO
        return dto;
    }

}