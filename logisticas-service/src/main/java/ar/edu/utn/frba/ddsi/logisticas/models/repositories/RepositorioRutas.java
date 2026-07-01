package ar.edu.utn.frba.ddsi.logisticas.models.repositories;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RepositorioRutas {
    private final List<Rutas> rutas = new ArrayList<>();

    public List<Rutas> findAll() {
        return new ArrayList<>(rutas);
    }

    public Optional<Ruta> findById(UUID id) {
        return rutas.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst();
    }

    public Ruta save(Ruta ruta) {
        rutas.add(ruta);
        return ruta;
    }

    public void deleteById(UUID id) {
        rutas.removeIf(r -> r.getId().equals(id));
    }
}
