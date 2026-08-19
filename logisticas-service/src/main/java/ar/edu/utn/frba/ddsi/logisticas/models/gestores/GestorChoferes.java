package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.dto.ChoferDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;

import java.util.List;
import java.util.UUID;

public class GestorChoferes {
    private final RepositorioChoferes repoChoferes;

    public GestorChoferes(RepositorioChoferes repoChoferes){
        this.repoChoferes = repoChoferes;
    }

    public List<Chofer>  listarChoferes(){
        return repoChoferes.findAll();
    }

    public Chofer buscarChofer(UUID id) {
        Chofer chofer = repoChoferes.findById(id);

        if (chofer == null) {
            throw new IllegalArgumentException("Chofer no encontrado");
        }

        return chofer;
    }

    public void guardarChofer(Chofer chofer){
        repoChoferes.save(chofer);
    }

    public void eliminarChofer(UUID id){
        Chofer chofer = buscarChofer(id);
        if(chofer != null){
            repoChoferes.deleteById(id);
        }
    }
}
