package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

/**
 * Repositorio en memoria para gestionar operaciones CRUD sobre objetos Administrador.
 */

@Repository
public class RepositorioAdministradores {
    private final List<Administrador> administradoresEnMemoria = new ArrayList<>();

    public RepositorioAdministradores() {}

    public void guardar(Administrador administrador) {
        if (administrador != null) {
            if (buscarPorId(administrador.getId()).isPresent()) {
                throw new IllegalArgumentException("Ya existe un administrador con el ID: " + administrador.getId());
            }
            this.administradoresEnMemoria.add(administrador);
        }
    }

    public List<Administrador> obtenerTodos() {
        return new ArrayList<>(this.administradoresEnMemoria);
    }

    public Optional<Administrador> buscarPorId(UUID id) {
        return this.administradoresEnMemoria.stream()
                                            .filter(a -> a.getId().equals(id))
                                            .findFirst();
    }

    public void actualizar(UUID idOriginal, Administrador adminActualizado) {
        Optional<Administrador> adminExistente = buscarPorId(idOriginal);
        if (adminExistente.isPresent()) {
            int index = this.administradoresEnMemoria.indexOf(adminExistente.get());
            this.administradoresEnMemoria.set(index, adminActualizado);
        } else {
            throw new IllegalArgumentException("No se encontró el administrador a actualizar.");
        }
    }

    public void eliminarPorId(UUID id) {
        this.administradoresEnMemoria.removeIf(a -> a.getId().equals(id));
    }
}
