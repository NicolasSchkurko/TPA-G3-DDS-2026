package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.EstadoRuta;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorChoferes;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorRutas;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RutaService {
  private final GestorRutas gestorRutas;
  private final GestorChoferes gestorChoferes;
  private final GestorItemEntrega gestorItemEntrega;
  private final GestorCamiones gestorCamiones;
  private final EventoLogisticaService eventoService;

  public RutaService(GestorRutas gestorRutas,
                     GestorChoferes gestorChoferes,
                     GestorItemEntrega gestorItemEntrega,
                     GestorCamiones gestorCamiones,
                     EventoLogisticaService eventoService) {
    this.gestorRutas = gestorRutas;
    this.gestorChoferes = gestorChoferes;
    this.gestorItemEntrega = gestorItemEntrega;
    this.gestorCamiones = gestorCamiones;
    this.eventoService = eventoService;
  }

  // --- MÉTODOS CRUD ---
  public List<Ruta> findAll() {
      return gestorRutas.listarRutas();
  }

  public Ruta findById(UUID idRuta) {
    return gestorRutas.buscarRuta(idRuta);
  }

  public Ruta create(Ruta ruta) {
    return gestorRutas.guardarRuta(ruta);
  }

  public Ruta update(UUID id, Ruta rutaActualizada) {
    return gestorRutas.actualizarRuta(id, rutaActualizada);
  }

  public void delete(UUID idRuta) {
    gestorRutas.eliminarRuta(idRuta);
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