package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class GestorCamiones {
    private final RepositorioCamiones repoCamiones;

    public GestorCamiones(RepositorioCamiones repoCamiones){
        this.repoCamiones = repoCamiones;
    }

    public Camion actualizarCamion(String patente, Chofer nuevoChofer, Double capacidadVolumen, Double altura, Double capacidadCarga, Boolean disponible){
        Camion camionExistente = repoCamiones.findById(patente)
                .orElseThrow(() -> new IllegalArgumentException("Camión no encontrado"));

        camionExistente.setChofer(nuevoChofer);
        camionExistente.setCapacidadVolumen(capacidadVolumen);
        camionExistente.setAltura(altura);
        camionExistente.setCapacidadCarga(capacidadCarga);
        camionExistente.setDisponible(disponible);

        repoCamiones.save(camionExistente);
        return camionExistente;
    }

    // --- MAPPERS ---
    public void resetearCamion(Camion camion){
        Optional<Camion> camionEncontrado = repoCamiones.findById(camion.getPatente());
        if(camionEncontrado.isPresent()){
            camionEncontrado.get().setCiudadDestinoActual(null);
            camionEncontrado.get().resetearCargaOcupada();
            repoCamiones.save(camionEncontrado.get());
        }
    }
}