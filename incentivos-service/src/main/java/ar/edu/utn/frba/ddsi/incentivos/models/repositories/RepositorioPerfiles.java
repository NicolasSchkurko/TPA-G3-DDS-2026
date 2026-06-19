package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Setter;
import lombok.Getter;

@Getter
@Setter

public class RepositorioPerfiles {
    private final List<Perfil> perfiles;

    private RepositorioPerfiles() {
        this.perfiles = new ArrayList<>();
    }

    public void agregarPerfil(Perfil perfil) {
        if (!perfiles.contains(perfil)) {
            perfiles.add(perfil);
        }
    }

    public void actualizar(Perfil perfilModificado) {
        if (perfilModificado == null || perfilModificado.getIdUsuario() == null) {
            return;
        }
        int index = perfiles.indexOf(perfilModificado);
        if (index >= 0) {
            perfiles.set(index, perfilModificado);
        }
    }

    public void eliminarPerfil(Perfil perfil) {
        perfiles.remove(perfil);
    }

    public List<Perfil> listarTodos() {
        return List.copyOf(perfiles);
    }

    public Perfil buscarPorIDUsuario(UUID id) {
        if (id == null || perfiles.isEmpty()) {
            return null;
        }
        return perfiles.stream()
                .filter(perfil -> id.equals(perfil.getIdUsuario()))
                .findFirst()
                .orElse(null);
    }

    public Perfil buscarPorIDPerfil(UUID id) {
        if (id == null || perfiles.isEmpty()) {
            return null;
        }
        return perfiles.stream()
                .filter(perfil -> id.equals(perfil.getIdPerfil()))
                .findFirst()
                .orElse(null);
    }
}