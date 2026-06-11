package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.MisionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.InsigniaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.ActividadMensualDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.MetricasActividad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.ActividadMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import org.springframework.stereotype.Service;

@Service
public class PerfilService {
    private final RepositorioPerfiles repositorioPerfiles = RepositorioPerfiles.getInstance();


    public PerfilDonanteDTO buscarPerfilPorUUID(UUID id) {
        Perfil entidad = repositorioPerfiles.buscarPorIDUsuario(id);
        if (entidad == null) {
            return null;
        }
        return mapToDTO(entidad);
    }

    public List<PerfilDonanteDTO> listarPerfiles() {
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
            throw new IllegalArgumentException("idUsuario no puede ser nulo");
        }

        // si ya existe un perfil con ese id, lanzar excepción
        if (repositorioPerfiles.buscarPorIDUsuario(idUsuario) != null) {
            throw new IllegalArgumentException("Ya existe un perfil con ese id");
        }

        Perfil nuevo = new Perfil(idUsuario);
        repositorioPerfiles.agregarPerfil(nuevo);
        return nuevo;
    }

    private PerfilDonanteDTO mapToDTO(Perfil perfil) {
        if (perfil == null) return null;

        PerfilDonanteDTO dto = new PerfilDonanteDTO();

        dto.setIdUsuario(perfil.getIdUsuario());
        dto.setNombreUsuario(perfil.getNombreUsuario());

        dto.setCategoria(perfil.getCategoriaActual().getNombre().name());

        dto.setTotalDonaciones(perfil.getTotalDonaciones());
        dto.setOrganizacionesAyudadas(perfil.getOrganizacionesAyudadas());
        dto.setPosicionRanking(perfil.getPosicionRanking());

        dto.setInsigniasObtenidas(perfil.getInsignias().stream().map(Insignia::toDTO).collect(Collectors.toList()));

        dto.setMisionActual(perfil.getMisionActual().toDTO());

        dto.setEvolucionMensual(perfil.getEvolucionMensual().stream().map(ActividadMensual::toDTO).collect(Collectors.toList()));

        dto.setMetricas(perfil.getMetricas().stream().map(MetricasActividad::toDTO).collect(Collectors.toList()));

        return dto;
    }
}