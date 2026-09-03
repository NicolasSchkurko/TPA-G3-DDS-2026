package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Role;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

@Repository
public class RepositorioPerfiles {
    private final List<Perfil> perfiles;

    public RepositorioPerfiles() {
        this.perfiles = new ArrayList<>();
    }

    public void agregarPerfil(Perfil perfil) {
        if (!perfiles.contains(perfil)) {
            perfiles.add(perfil);
        }
    }

    public Perfil actualizar(Perfil perfilModificado) {
        if (perfilModificado == null || perfilModificado.getIdUsuario() == null) {
            return null;
        }

        Perfil existente = this.buscarPorIDUsuario(perfilModificado.getIdUsuario());
        if (existente != null) {
            // Actualizar solo los campos no nulos del perfilModificado
            if (perfilModificado.getNombreUsuario() != null) {
                existente.setNombreUsuario(perfilModificado.getNombreUsuario());
            }
            if (perfilModificado.getCategoriaActual() != null) {
                existente.setCategoriaActual(perfilModificado.getCategoriaActual());
            }
            if (perfilModificado.getInsignias() != null) {
                existente.setInsignias(perfilModificado.getInsignias());
            }
            if (perfilModificado.getMisionActual() != null) {
                existente.setMisionActual(perfilModificado.getMisionActual());
            }
            if (perfilModificado.getPosicionRanking() != null) {
                existente.setPosicionRanking(perfilModificado.getPosicionRanking());
            }

            int index = perfiles.indexOf(existente);
            if (index >= 0) {
                perfiles.set(index, existente);
            }
        }
        return existente;
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

    public void verificarProgresos() {
        this.listarTodos().stream()
                   .filter(perfil -> perfil.getRole() == Role.USER)
                   .filter(perfil -> perfil.getMisionActual().getReglaDeProgreso().getConstancia() != null)
                   .forEach(Perfil::verificarProgresoMision);
    }

    public Mision obtenerMisionPerfil(UUID idUsuario){
        Perfil perfil = this.buscarPorIDUsuario(idUsuario);

        return perfil.getMisionActual();
    }

    public List<Insignia> obtenerInsigniasPerfil(UUID idUsuario){
        Perfil perfil = this.buscarPorIDUsuario(idUsuario);

        return perfil.getInsignias();
    }
}