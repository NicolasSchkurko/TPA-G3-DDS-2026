package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RepositorioCamiones {
    private final List<Camion> camiones = new ArrayList<>();

    public List<Camion> findAll() {
        return new ArrayList<>(camiones);
    }

    public Optional<Camion> findById(String patente) {
        return camiones.stream()
                       .filter(c -> c.getPatente().equals(patente))
                       .findFirst();
    }

    public Optional<Camion> findByChoferId(UUID idChofer) {
        if (idChofer == null) return Optional.empty();
        return camiones.stream()
                       .filter(camion -> camion.getChofer() != null &&
                           idChofer.equals(camion.getChofer().getIdChofer()))
                       .findFirst();
    }

    public Camion save(Camion camion) {
        int posicion = camiones.indexOf(camion);
        if (posicion != -1) {
            camiones.set(posicion, camion);
        } else {
            camiones.add(camion);
        }
        return camion;
    }

    public void resetearCarga(Camion camion){
        int posicion = camiones.indexOf(camion);
        if (posicion != -1) {
            camion.setCiudadDestinoActual(null);
            camion.resetearCargaOcupada();
            camiones.set(posicion, camion);
        }
    }

    public void deleteById(String patente) {
        camiones.removeIf(c -> c.getPatente().equals(patente));
    }
}