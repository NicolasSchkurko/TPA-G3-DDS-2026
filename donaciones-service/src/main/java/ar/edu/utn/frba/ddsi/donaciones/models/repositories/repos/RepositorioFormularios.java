package ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.FormularioJpaRepository;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    public Page<Formulario> obtenerTodos(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    public Optional<Formulario> buscarPorId(UUID id) {
        return jpaRepository.findById(id);
    }

    public void desvincularDonante(UUID donanteId) {
        jpaRepository.desvincularDonante(donanteId);
    }

    public void eliminarPorId(UUID id) {
        jpaRepository.deleteById(id);
    }
}
