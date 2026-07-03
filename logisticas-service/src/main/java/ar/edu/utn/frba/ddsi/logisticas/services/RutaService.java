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

import java.util.UUID;

@Service
public class RutaService {
  private final RepositorioRutas repositorioRutas;
  private final RepositorioChoferes repositorioChoferes;
  private final RepositorioItemEntrega repositorioItemEntrega;
  private final RepositorioCamiones repositorioCamiones;

  public RutaService(RepositorioRutas repositorioRutas, RepositorioChoferes repositorioChoferes,
                     RepositorioItemEntrega repositorioItemEntrega, RepositorioCamiones repositorioCamiones) {
    this.repositorioRutas = repositorioRutas;
    this.repositorioChoferes = repositorioChoferes;
    this.repositorioItemEntrega = repositorioItemEntrega;
    this.repositorioCamiones = repositorioCamiones;
  }

  public Ruta obtenerRutaDeChofer(Chofer chofer) {
    if (chofer == null) return null;
    return repositorioRutas.findAll().stream()
                           .filter(ruta -> ruta.getCamionAsignado() != null &&
                               chofer.equals(ruta.getCamionAsignado().getChofer()))
                           .findFirst()
                           .orElse(null);
  }

  public Camion obtenerCamionDeChofer(UUID idChofer) {
    if (idChofer == null) return null;
    return repositorioCamiones.findAll().stream()
                              .filter(camion -> camion.getChofer() != null &&
                                  idChofer.equals(camion.getChofer().getIdChofer()))
                              .findFirst()
                              .orElse(null);
  }

  public void terminarRuta(UUID idChofer) {
    Chofer chofer = repositorioChoferes.findById(idChofer);
    if (chofer == null) return;

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
      repositorioCamiones.actualizarcarga(camion);
    }

    repositorioChoferes.actualizarCamion(chofer);
  }

  public void iniciarRuta(UUID idChofer) {
    Chofer chofer = repositorioChoferes.findById(idChofer);
    if (chofer == null) return;

    Ruta rutaActual = this.obtenerRutaDeChofer(chofer);

    if (rutaActual != null) {
      repositorioRutas.actualizarEstado(rutaActual, EstadoRuta.EN_CURSO);

      rutaActual.getParadas().forEach(parada ->
                                          parada.getItems().forEach(item ->
                                                                        repositorioItemEntrega.actualizarEstado(item, EstadoEntrega.EN_TRASLADO)
                                          )
      );
    }
  }
}