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

    // 1. Delegamos en el Dominio (tus clases puras)
    ruta.iniciar(); // Pasa ruta a EN_CURSO y donaciones a EN_TRASLADO

    // 2. Guardamos en la Base de Datos (en este caso, en memoria)
    repositorioRutas.save(ruta);

    // 3. Generamos el evento asíncrono para el HTTP Polling
    eventoService.publicarInicioRuta(ruta);
  }

  /**
   * Caso de uso: La entidad confirma que el camión le dejó las cajas.
   */
  public void registrarEntregaConfirmada(UUID idDonacion, String fotoUrl) {
    ItemEntrega item = repositorioItemEntrega.findById(idDonacion)
                                             .orElseThrow(() -> new IllegalArgumentException("Donación no encontrada"));

    // 1. Lógica de Dominio
    item.confirmarEntrega(fotoUrl);

    // 2. Persistencia
    repositorioItemEntrega.save(item);

    // 3. Generamos el evento para Notificaciones
    eventoService.publicarEntregaConfirmada(item);
  }
}