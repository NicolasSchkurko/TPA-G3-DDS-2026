package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.EstadoRuta;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RutaService {
  private final RepositorioRutas repositorioRutas;
  private final RepositorioChoferes repositorioChoferes;
  private final RepositorioItemEntrega repositorioItemEntrega;
  private final RepositorioCamiones repositorioCamiones;
  private final EventoLogisticaService eventoService;

  public RutaService(RepositorioRutas repositorioRutas,
                     RepositorioChoferes repositorioChoferes,
                     RepositorioItemEntrega repositorioItemEntrega,
                     RepositorioCamiones repositorioCamiones,
                     EventoLogisticaService eventoService) {
    this.repositorioRutas = repositorioRutas;
    this.repositorioChoferes = repositorioChoferes;
    this.repositorioItemEntrega = repositorioItemEntrega;
    this.repositorioCamiones = repositorioCamiones;
    this.eventoService = eventoService;
  }

  // --- MÉTODOS CRUD ---
  public List<Ruta> findAll() {
    return repositorioRutas.findAll();
  }

  public Ruta findById(UUID idRuta) {
    return repositorioRutas.findById(idRuta)
                           .orElseThrow(() -> new IllegalArgumentException("Ruta no encontrada"));
  }

  public Ruta create(Ruta ruta) {
    return repositorioRutas.save(ruta);
  }

  public Ruta update(UUID id, Ruta rutaActualizada) {
    Ruta rutaExistente = findById(id);
    rutaExistente.setCamionAsignado(rutaActualizada.getCamionAsignado());
    rutaExistente.setParadas(rutaActualizada.getParadas());
    return repositorioRutas.save(rutaExistente);
  }

  public void delete(UUID idRuta) {
    if (repositorioRutas.findById(idRuta).isEmpty()) {
      throw new IllegalArgumentException("Ruta no encontrada");
    }
    repositorioRutas.deleteById(idRuta);
  }

  // --- MÉTODOS DE NEGOCIO ---
  public Ruta obtenerRutaDeChofer(Chofer chofer) {
    if (chofer == null) return null;
    return repositorioRutas.findByChofer(chofer).orElse(null);
  }

  public Camion obtenerCamionDeChofer(UUID idChofer) {
    if (idChofer == null) return null;
    return repositorioCamiones.findByChoferId(idChofer).orElse(null);
  }

  public void iniciarRuta(UUID idChofer) {
    Chofer chofer = repositorioChoferes.findById(idChofer);
    if (chofer == null) throw new IllegalArgumentException("Chofer no encontrado");

    Ruta rutaActual = this.obtenerRutaDeChofer(chofer);
    if (rutaActual == null) throw new IllegalStateException("El chofer no tiene ninguna ruta asignada");

    repositorioRutas.actualizarEstado(rutaActual, EstadoRuta.EN_CURSO);

    rutaActual.getParadas().forEach(parada ->
                                        parada.getItems().forEach(item -> {
                                          repositorioItemEntrega.actualizarEstado(item, EstadoEntrega.EN_TRASLADO);
                                        })
    );

    eventoService.publicarInicioRuta(rutaActual);
  }

  public void terminarRuta(UUID idChofer) {
    Chofer chofer = repositorioChoferes.findById(idChofer);
    if (chofer == null) throw new IllegalArgumentException("Chofer no encontrado");

    Ruta rutaActual = obtenerRutaDeChofer(chofer);

    if (rutaActual != null) {
      repositorioRutas.actualizarEstado(rutaActual, EstadoRuta.FINALIZADA);
      for(Parada parada : rutaActual.getParadas()){
        for(ItemEntrega item : parada.getItems()){
          if(item.getEstado() != EstadoEntrega.ENTREGADA){
            repositorioItemEntrega.actualizarEstado(item, EstadoEntrega.PENDIENTE);
          } else {
            repositorioItemEntrega.deleteById(item.getIdDonacion());
          }
        }
      }
    }

    Camion camion = obtenerCamionDeChofer(idChofer);
    if (camion != null) {
      camion.eliminarChofer();
      repositorioCamiones.actualizarcarga(camion);
    }
  }
}