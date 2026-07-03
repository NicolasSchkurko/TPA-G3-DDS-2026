package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioEventoLogistica;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventoLogisticaService {

  private final RepositorioEventoLogistica repositorioEventos;

  public EventoLogisticaService(RepositorioEventoLogistica repositorioEventos) {
    this.repositorioEventos = repositorioEventos;
  }

  public List<EventoLogistica> obtenerEventosNuevos(Long desdeId) {
    return repositorioEventos.findByIdGreaterThanOrderByFechaAsc(desdeId);
  }

  public void publicarInicioRuta(Ruta ruta) {
    EventoLogistica evento = new EventoLogistica("INICIO_RUTA", ruta.getIdRuta().toString(), LocalDateTime.now(), null);
    repositorioEventos.save(evento);
  }

  public void publicarEntregaConfirmada(ItemEntrega item) {
    EventoLogistica evento = new EventoLogistica("ENTREGA_CONFIRMADA", item.getIdDonacion().toString(), LocalDateTime.now(), null);
    repositorioEventos.save(evento);
  }

  // Adaptado para recibir la justificación separada y persistirla en el evento
  public void publicarEntregaFallida(ItemEntrega item, String justificacion) {
    EventoLogistica evento = new EventoLogistica("ENTREGA_FALLIDA", item.getIdDonacion().toString(), LocalDateTime.now(), justificacion);
    repositorioEventos.save(evento);
  }

  public void publicarReingresoDeposito(ItemEntrega item) {
    EventoLogistica evento = new EventoLogistica("REINGRESO_DEPOSITO", item.getIdDonacion().toString(), LocalDateTime.now(), null);
    repositorioEventos.save(evento);
  }
}