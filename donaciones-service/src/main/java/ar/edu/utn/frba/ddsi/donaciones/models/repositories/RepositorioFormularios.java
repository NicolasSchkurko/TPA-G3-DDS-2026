package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RepositorioFormularios  {
    // Simulamos una base de datos en memoria
    private final List<Formulario> formularios = new ArrayList<>();

    public List<Formulario> findAll() {
        return new ArrayList<>(formularios);
    }

    public Optional<Formulario> findById(UUID id) {
        return formularios.stream()
                .filter(f -> f.getDonante().getId().equals(id))
                .findFirst();
    }

    public Formulario save(Formulario formulario) {
        formularios.add(formulario);
        return formulario;
    }

    public void deleteById(UUID id) {
        formularios.removeIf(f -> f.getDonante().getId().equals(id));
    }
}