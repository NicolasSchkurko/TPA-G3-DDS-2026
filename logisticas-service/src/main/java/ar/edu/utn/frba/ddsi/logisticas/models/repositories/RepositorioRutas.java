package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RepositorioRutas {
    private final List<Ruta> rutas = new ArrayList<>();

    public List<Ruta> findAll() {
        return new ArrayList<>(rutas);
    }

    public Optional<Ruta> findById(UUID id) {
        return rutas.stream()
                .filter(r -> r.getIdRuta().equals(id))
                .findFirst();
    }

    public void saveAll(List<Ruta> listaRutas) {
        rutas.addAll(listaRutas);
    }

    public Ruta save(Ruta ruta) {
        rutas.add(ruta);
        return ruta;
    }

    public void deleteById(UUID id) {
        rutas.removeIf(r -> r.getIdRuta().equals(id));
    }
}
