package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TrazabilidadService {

  private final RepositorioRutas repositorioRutas;
  private final RepositorioItemEntrega repositorioItemEntrega;
  private final EventoLogisticaService eventoService;

  public TrazabilidadService(RepositorioRutas repositorioRutas,
                             RepositorioItemEntrega repositorioItemEntrega,
                             EventoLogisticaService eventoService) {
    this.repositorioRutas = repositorioRutas;
    this.repositorioItemEntrega = repositorioItemEntrega;
    this.eventoService = eventoService;
  }

  /**
   * Caso de uso: El chofer presiona "Iniciar Ruta" en su aplicación.
   */
  public void registrarInicioRuta(UUID idRuta) {
    Ruta ruta = repositorioRutas.findById(idRuta)
                                .orElseThrow(() -> new IllegalArgumentException("Ruta no encontrada"));

    ruta.iniciar();
    repositorioRutas.save(ruta);
    eventoService.publicarInicioRuta(ruta);
  }

  /**
   * Caso de uso: La entidad confirma que el camión le dejó las cajas.
   */
  public void registrarEntregaConfirmada(UUID idDonacion, String fotoUrl) {
    ItemEntrega item = repositorioItemEntrega.findById(idDonacion)
                                             .orElseThrow(() -> new IllegalArgumentException("Donación no encontrada"));

    item.confirmarEntrega(fotoUrl);
    repositorioItemEntrega.save(item);
    eventoService.publicarEntregaConfirmada(item);
  }

  /**
   * Caso de uso: La entidad reporta que la donación no llegó (falla logística).
   */
  public void registrarEntregaFallida(UUID idDonacion) {
    ItemEntrega item = repositorioItemEntrega.findById(idDonacion)
                                             .orElseThrow(() -> new IllegalArgumentException("Donación no encontrada"));

    item.marcarNoRecibida();
    repositorioItemEntrega.save(item);
    eventoService.publicarEntregaFallida(item);
  }

  /**
   * Caso de uso: Tras una falla, un revisor determina que la donación volvió al depósito y puede ser enviada nuevamente.
   */
  public void registrarReingresoDeposito(UUID idDonacion) {
    ItemEntrega item = repositorioItemEntrega.findById(idDonacion)
                                             .orElseThrow(() -> new IllegalArgumentException("Donación no encontrada"));

    item.reingresarADeposito();
    repositorioItemEntrega.save(item);
    eventoService.publicarReingresoDeposito(item);
  }
}