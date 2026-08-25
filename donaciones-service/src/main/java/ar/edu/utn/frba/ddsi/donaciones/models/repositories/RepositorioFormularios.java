package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RepositorioFormularios {
    private List<Formulario> formularios;

    public RepositorioFormularios() {
        this.formularios = new ArrayList<>();
    }

    public void guardar(Formulario formulario) {
        if (formulario != null && formulario.getId() != null) {
            if (buscarPorId(formulario.getId()).isPresent()) {
                throw new IllegalArgumentException("Ya existe un donante con el ID: " + formulario.getId());
            }
            this.formularios.add(formulario);
        }
    }

    public List<Formulario> obtenerTodos() {
        return new ArrayList<>(this.formularios);
    }

    public Optional<Formulario> buscarPorId(UUID id) {
        return this.formularios.stream()
                .filter(f -> f.getId().equals(id))
                .findFirst();
    }

    public void eliminarPorId(UUID id) {
        this.formularios.removeIf(f -> f.getId().equals(id));
    }
}