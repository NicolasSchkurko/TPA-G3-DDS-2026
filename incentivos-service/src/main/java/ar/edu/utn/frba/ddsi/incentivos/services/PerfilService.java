package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.PerfilDuplicadoException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.MetricasActividad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.ActividadMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import org.springframework.stereotype.Service;

@Service
public class PerfilService {
    private final RepositorioPerfiles repositorioPerfiles = RepositorioPerfiles.getInstance();

//    private final ImpactoDonacionService impactoDonacionService;
//
//    public PerfilService(ImpactoDonacionService impactoDonacionService) {
//        this.impactoDonacionService = impactoDonacionService;
//    }
//
//    public void procesarDonacion(
//            UUID idDonacion) {
//
//        ImpactoDonacionDTO donacion =
//                impactoDonacionService
//                        .buscarDonacionPorUUID(idDonacion);
//
//        Perfil perfil =
//                repositorioPerfiles.buscarPorIDUsuario(donacion.getIdUsuario());
//
//        perfil.progresarMision(donacion);
//
//        perfilRepository
//                .actualizar(perfil);
//    }
//    public void registrarDonacionEnPerfil(UUID idUsuario, Donacion nuevaDonacion) {
//        Perfil perfil = repositorioPerfiles.buscarPorIDUsuario(idUsuario);
//        if (perfil != null) {
//            perfil.verificarProgresoMision(nuevaDonacion);
//
//            repositorioPerfiles.actualizar(perfil);
//        }
//    }

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

    public Perfil crearPerfil(UUID idUsuario) {
        if (idUsuario == null) {
            throw new DatosInvalidosException();
        }

        if (repositorioPerfiles.buscarPorIDUsuario(idUsuario) != null) {
            throw new PerfilDuplicadoException();
        }

        Perfil nuevo = new Perfil(idUsuario);
        repositorioPerfiles.agregarPerfil(nuevo);
        return nuevo;
    }

    private PerfilNotificacionDTO mapToDTO(Perfil perfil) {
        if (perfil == null) return null;

        PerfilNotificacionDTO dto = new PerfilNotificacionDTO();
        //TODO
        return dto;
    }
}