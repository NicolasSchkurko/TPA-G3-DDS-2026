package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.InsigniaObtenidaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.MisionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilDonanteDTO;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import org.springframework.stereotype.Service;

@Service
public class PerfilService {
    private final RepositorioPerfiles repositorioPerfiles = RepositorioPerfiles.getInstance();

    public PerfilDonanteDTO obtenerPerfil(String nombreUsuario) {
        Perfil perfil = repositorioPerfiles.buscarPorNombreUsuario(nombreUsuario);

        if (perfil == null) {
            return null;
        }

        PerfilDonanteDTO dto = new PerfilDonanteDTO();
        dto.setNombreUsuario(perfil.getNombreUsuario());
        if (perfil.getCategoriaActual() != null && perfil.getCategoriaActual().getNombre() != null) {
            dto.setCategoria(perfil.getCategoriaActual().getNombre().name());
        } else {
            dto.setCategoria(null);
        }

        return dto;
    }
}
