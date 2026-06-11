package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class DonacionService {
    private final RepositorioPerfiles repositorioPerfiles = RepositorioPerfiles.getInstance();

    public PerfilDonanteDTO obtenerPerfil(UUID id) {
        Perfil perfil = repositorioPerfiles.buscarPorIDUsuario(id);

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
