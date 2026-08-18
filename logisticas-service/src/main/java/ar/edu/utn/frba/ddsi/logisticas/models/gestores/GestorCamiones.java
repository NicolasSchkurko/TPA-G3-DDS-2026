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
    private final GestorChoferes gestorChoferes;

    public GestorCamiones(RepositorioCamiones repoCamiones, GestorChoferes gestorChoferes){
        this.repoCamiones = repoCamiones;
        this.gestorChoferes = gestorChoferes;
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
    public Camion nuevoCamion(CamionDTO dto){
        if (dto == null) return null;
        var chofer = dto.getIdChofer() != null ? gestorChoferes.buscarChofer(dto.getIdChofer()) : null;
        return new Camion(chofer, dto.getPatente(), dto.getCapacidadVolumen(),
                dto.getAltura(), dto.getCapacidadCarga(), dto.getDisponible());
    }

    public Camion actualizarCamion(String patente, CamionDTO dto){
        Camion camionExistente = this.buscarCamion(patente);

        Chofer nuevoChofer = dto.getIdChofer() != null ? gestorChoferes.buscarChofer(dto.getIdChofer()) : null;

        camionExistente.setChofer(nuevoChofer);
        camionExistente.setCapacidadVolumen(dto.getCapacidadVolumen());
        camionExistente.setAltura(dto.getAltura());
        camionExistente.setCapacidadCarga(dto.getCapacidadCarga());

        this.guardarCamion(camionExistente);
        return camionExistente;
    }

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