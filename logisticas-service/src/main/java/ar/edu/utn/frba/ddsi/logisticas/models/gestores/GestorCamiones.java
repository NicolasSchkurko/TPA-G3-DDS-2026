package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import org.springframework.stereotype.Component;

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
        if (capacidadVolumen != null) camionExistente.setCapacidadVolumen(capacidadVolumen);
        if (altura != null) camionExistente.setAltura(altura);
        if (capacidadCarga != null) camionExistente.setCapacidadCarga(capacidadCarga);
        if (disponible != null) camionExistente.setDisponible(disponible);

        repoCamiones.save(camionExistente);
        return camionExistente;
    }

    // --- MAPPERS ---
    public void resetearCamion(Camion camion) {
        camion.eliminarChofer();
        camion.disponible();
        camion.resetearCargaOcupada();
    }
}