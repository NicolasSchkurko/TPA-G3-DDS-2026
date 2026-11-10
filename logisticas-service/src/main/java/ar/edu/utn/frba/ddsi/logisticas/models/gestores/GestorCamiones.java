package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import org.springframework.stereotype.Component;

@Component
public class GestorCamiones {

    public Camion actualizarCamion(Camion camionExistente, Chofer nuevoChofer,
                                   Double capacidadVolumen, Double altura,
                                   Double capacidadCarga, Boolean disponible) {
        // Null significa "no vino en el request": se conserva el chofer asignado. Desvincular
        // es una operación explícita (resetearCamion/eliminarChofer), no un efecto de mandar
        // solo la disponibilidad o la capacidad.
        if (nuevoChofer != null) camionExistente.setChofer(nuevoChofer);
        if (capacidadVolumen != null) camionExistente.setCapacidadVolumen(capacidadVolumen);
        if (altura != null) camionExistente.setAltura(altura);
        if (capacidadCarga != null) camionExistente.setCapacidadCarga(capacidadCarga);
        if (disponible != null) camionExistente.setDisponible(disponible);

        return camionExistente;
    }

    public void resetearCamion(Camion camion) {
        camion.eliminarChofer();
        camion.disponible();
        camion.resetearCargaOcupada();
    }
}
