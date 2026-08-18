package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.EstadoRuta;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;

import java.util.List;
import java.util.UUID;

public class GestorRutas {
    private final RepositorioRutas repoRutas;

    public GestorRutas(RepositorioRutas repoRutas){
        this.repoRutas = repoRutas;
    }

    public List<Ruta> listarRutas(){
        return repoRutas.findAll();
    }

    public Ruta buscarRuta(UUID id){
        return repoRutas.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ruta no encontrado"));
    }

    public Ruta buscarRutaDeIdDonacion(UUID idDonacion){
        return repoRutas.findByIdDonacion(idDonacion)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la ruta correspondiente a la donación " + idDonacion));
    }

    public Ruta buscarRutaPorChofer(Chofer chofer){
        return repoRutas.findByChofer(chofer)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la ruta correspondiente al chofer " + chofer.getIdChofer()));
    }

    public Ruta guardarRuta(Ruta nuevaRuta){
        repoRutas.save(nuevaRuta);
        return nuevaRuta;
    }

    public void guardarRutas(List<Ruta> todosLosIdRutas){
        todosLosIdRutas.forEach(this::guardarRuta);
    }

    public Ruta actualizarRuta(UUID id, Ruta rutaActualizada){
        Ruta rutaExistente = buscarRuta(id);
        rutaExistente.setCamionAsignado(rutaActualizada.getCamionAsignado());
        rutaExistente.setParadas(rutaActualizada.getParadas());
        return guardarRuta(rutaExistente);
    }

    public void actualizarRutaEstado(Ruta rutaActual, EstadoRuta nuevoEstado){
        repoRutas.actualizarEstado(rutaActual, nuevoEstado);
    }

    public void eliminarRuta(UUID idRuta){
        buscarRuta(idRuta);
        repoRutas.deleteById(idRuta);
    }
}
