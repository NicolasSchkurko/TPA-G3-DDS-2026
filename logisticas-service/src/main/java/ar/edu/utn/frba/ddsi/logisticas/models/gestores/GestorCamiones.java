package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class GestorCamiones {
    private final RepositorioCamiones repoCamiones;

    public GestorCamiones(RepositorioCamiones repoCamiones){
        this.repoCamiones = repoCamiones;
    }

    public List<Camion> listarCamiones(){
        return repoCamiones.findAll();
    }

    public Camion buscarCamion(String patente){
        return repoCamiones.findById(patente)
                .orElseThrow(() -> new IllegalArgumentException("Camión no encontrado"));
    }

    public Camion buscarCamionPorIdChofer(UUID idchofer){
        return repoCamiones.findByChoferId(idchofer)
                .orElseThrow(() -> new IllegalArgumentException("Camión no encontrado"));
    }

    public void guardarCamion(Camion camion){
        repoCamiones.save(camion);
    }

    public Camion actualizarCamion(String patente, CamionDTO dto, Chofer nuevoChofer) {
        Camion camionExistente = buscarCamion(patente);

        camionExistente.setChofer(nuevoChofer);
        camionExistente.setCapacidadVolumen(dto.getCapacidadVolumen());
        camionExistente.setAltura(dto.getAltura());
        camionExistente.setCapacidadCarga(dto.getCapacidadCarga());

        guardarCamion(camionExistente);
        return camionExistente;
    }

    // --- MAPPERS ---
    public void resetearCamion(Camion camion){
        repoCamiones.resetearCarga(camion);
    }

    public void eliminarCamion(String patente) {
        Camion camion = buscarCamion(patente);
        if(camion != null){
            repoCamiones.deleteById(patente);
        }
    }

    public void marcarDisponible(String patente) {
        Camion camion = buscarCamion(patente);
        camion.disponible();
        guardarCamion(camion);
    }

    public void marcarOcupado(String patente) {
        Camion camion = buscarCamion(patente);
        camion.ocupado();
        guardarCamion(camion);
    }
}