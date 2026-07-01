package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.RutaEnProceso;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioRutasActivas {
    private final List<RutaEnProceso> rutas = new ArrayList<>();

    public List<RutaEnProceso> findAll() {
        return new ArrayList<>(rutas);
    }

    public Optional<RutaEnProceso> findByIdRuta(UUID idRuta) {
        return rutas.stream()
                .filter(ruta -> ruta.getIdRuta().equals(idRuta))
                .findFirst();
    }

    public Optional<RutaEnProceso> findByIdEntrega(UUID idEntrega) {
        return rutas.stream()
                .filter(ruta -> ruta.getIdEntrega().equals(idEntrega))
                .findFirst();
    }

    public RutaEnProceso save(RutaEnProceso ruta) {
        findByIdRuta(ruta.getIdRuta()).ifPresent(rutas::remove);
        rutas.add(ruta);
        return ruta;
    }

    public void deleteByIdEntrega(UUID idEntrega) {
        rutas.removeIf(ruta -> ruta.getIdEntrega().equals(idEntrega));
    }
}
