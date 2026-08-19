package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.dto.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;

import java.util.List;
import java.util.UUID;

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
}