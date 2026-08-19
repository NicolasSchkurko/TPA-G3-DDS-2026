package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.EstadoRuta;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorChoferes;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorRutas;

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

  public void iniciarRuta(UUID idChofer) {
    Ruta rutaActual = gestorRutas.buscarRutaPorChofer(gestorChoferes.buscarChofer(idChofer));
    gestorRutas.actualizarRutaEstado(rutaActual, EstadoRuta.EN_CURSO);

    eventoService.publicarInicioRuta(rutaActual);
  }

  public void terminarRuta(UUID idChofer) {
    Ruta rutaActual = gestorRutas.buscarRutaPorChofer(gestorChoferes.buscarChofer(idChofer));

    if (rutaActual != null) {
      gestorRutas.actualizarRutaEstado(rutaActual, EstadoRuta.FINALIZADA);
      for(Parada parada : rutaActual.getParadas()){
        for(ItemEntrega item : parada.getItems()){
          if (item.getEstado() == EstadoEntrega.ENTREGADA) {
            eventoService.publicarReingresoDeposito(item);
          } else {
            gestorItemEntrega.eliminarItem(item.getIdDonacion());
          }
        }
      }
    }

    Camion camion = gestorCamiones.buscarCamionPorIdChofer(idChofer);
    if (camion != null) {
      camion.eliminarChofer();
      gestorCamiones.resetearCamion(camion);
    }
  }
}