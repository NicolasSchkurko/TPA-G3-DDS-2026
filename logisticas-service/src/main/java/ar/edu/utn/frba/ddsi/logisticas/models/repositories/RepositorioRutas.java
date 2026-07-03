package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.EstadoRuta;
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

    public Optional<Ruta> findByChofer(Chofer chofer) {
        if (chofer == null) return Optional.empty();
        return rutas.stream()
                    .filter(ruta -> ruta.getCamionAsignado() != null &&
                        chofer.equals(ruta.getCamionAsignado().getChofer()))
                    .findFirst();
    }

    public Ruta save(Ruta ruta) {
        int posicion = rutas.indexOf(ruta);
        if (posicion != -1) {
            rutas.set(posicion, ruta);
        } else {
            rutas.add(ruta);
        }
        return ruta;
    }

    public void saveAll(List<Ruta> listaRutas) {
        rutas.addAll(listaRutas);
    }

    public void actualizarEstado(Ruta ruta, EstadoRuta nuevoEstado){
        int posicion = rutas.indexOf(ruta);
        if (posicion != -1) {
            ruta.setEstado(nuevoEstado);
            rutas.set(posicion, ruta);
        }
    }

    public void deleteById(UUID id) {
        rutas.removeIf(r -> r.getIdRuta().equals(id));
    }
}