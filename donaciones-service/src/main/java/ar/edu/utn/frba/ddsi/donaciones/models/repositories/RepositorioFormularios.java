package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fachada sobre FormularioJpaRepository (Spring Data JPA).
 * Mantiene la misma interfaz pública que tenía cuando era un repositorio en memoria.
 */
@Repository
public class RepositorioFormularios {

    private final FormularioJpaRepository jpaRepository;

    public RepositorioFormularios(FormularioJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public void guardar(Formulario formulario) {
        if (formulario != null && formulario.getId() != null) {
            if (jpaRepository.existsById(formulario.getId())) {
                throw new IllegalArgumentException("Ya existe un formulario con el ID: " + formulario.getId());
            }
            jpaRepository.save(formulario);
        }
    }

    public List<Formulario> obtenerTodos() {
        return jpaRepository.findAll();
    }

    public Optional<Formulario> buscarPorId(UUID id) {
        return jpaRepository.findById(id);
    }

    public void eliminarPorId(UUID id) {
        jpaRepository.deleteById(id);
    }
}
