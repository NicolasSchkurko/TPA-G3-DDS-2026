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

    // NUEVO MÉTODO: Filtra la ruta directamente por el chofer asignado a su camión
    public Optional<Ruta> findByChofer(Chofer chofer) {
        if (chofer == null) return Optional.empty();
        return rutas.stream()
                    .filter(ruta -> ruta.getCamionAsignado() != null &&
                        chofer.equals(ruta.getCamionAsignado().getChofer()))
                    .findFirst();
    }

    public void saveAll(List<Ruta> listaRutas) {
        rutas.addAll(listaRutas);
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

    public Ruta findByItem(ItemEntrega item) {
        return rutas.stream()
                    .filter(ruta -> ruta.getParadas().stream()
                                        .anyMatch(parada -> parada.getItems().contains(item)))
                    .findFirst()
                    .orElse(null);
    }

    public void actualizarEstado(Ruta ruta, EstadoRuta nuevoEstado){
        int posicion = rutas.indexOf(ruta);
        ruta.setEstado(nuevoEstado);
        if (posicion != -1) {
            rutas.set(posicion, ruta);
        }
    }

    public void actualizarChofer(Ruta ruta, Chofer chofer){
        int posicion = rutas.indexOf(ruta);
        if (ruta.getCamionAsignado() != null) {
            ruta.getCamionAsignado().setChofer(chofer);
        }
        if (posicion != -1) {
            rutas.set(posicion, ruta);
        }
    }

    public void deleteById(UUID id) {
        rutas.removeIf(r -> r.getIdRuta().equals(id));
    }
}