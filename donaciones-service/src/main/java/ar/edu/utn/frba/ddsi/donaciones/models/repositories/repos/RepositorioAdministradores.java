package ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.AdministradorJpaRepository;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Fachada sobre AdministradorJpaRepository (Spring Data JPA).
 * Mantiene la misma interfaz pública que tenía cuando era un repositorio en memoria
 * (incluyendo que buscarPorId lanza si no encuentra, tal cual el comportamiento original).
 */
@Repository
public class RepositorioAdministradores {

    private final AdministradorJpaRepository jpaRepository;

    public RepositorioAdministradores(AdministradorJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public void guardar(Administrador administrador) {
        if (administrador != null) {
            if (jpaRepository.existsById(administrador.getId())) {
                throw new IllegalArgumentException("Ya existe un administrador con el ID: " + administrador.getId());
            }
            jpaRepository.save(administrador);
        }
    }

    public List<Administrador> obtenerTodos() {
        return jpaRepository.findAll();
    }

    /** Busca por id. Devuelve {@code Optional.empty()} si no existe, no lanza: un repositorio
     *  devuelve el dato, no decide si es un error. Si lanzara IAE, el proxy de
     *  {@code @Repository} lo traduciría a {@code InvalidDataAccessApiUsageException}, que
     *  escapa al {@code catch (IllegalArgumentException)} del controller y termina en 500. */
    public Optional<Administrador> buscarPorId(UUID id) {
        return jpaRepository.findById(id);
    }

    public void actualizar(UUID idOriginal, Administrador adminActualizado) {
        if (jpaRepository.existsById(idOriginal)) {
            jpaRepository.save(adminActualizado);
        } else {
            throw new IllegalArgumentException("No se encontró el administrador a actualizar.");
        }
    }

    public void eliminarPorId(UUID id) {
        jpaRepository.deleteById(id);
    }
}
