package ar.edu.utn.frba.ddsi.logisticas.services;


import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioEventoLogistica;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EventoLogisticaService {

  private final RepositorioEventoLogistica repositorioEventos;
  private final ObjectMapper objectMapper;

  public EventoLogisticaService(RepositorioEventoLogistica repositorioEventos) {
    this.repositorioEventos = repositorioEventos;
    this.objectMapper = new ObjectMapper();
  }

  public List<EventoLogistica> obtenerEventosNuevos(Long ultimoIdProcesado) {
    return repositorioEventos.findByIdGreaterThanOrderByFechaAsc(ultimoIdProcesado);
  }

  public void publicarInicioRuta(Ruta ruta) {
    try {
      Map<String, Object> payload = new HashMap<>();
      payload.put("idRuta", ruta.getIdRuta());
      payload.put("patenteCamion", ruta.getCamionAsignado().getPatente());
      payload.put("items", ruta.obtenerTodosLosItems().stream()
                               .map(ItemEntrega::getIdDonacion)
                               .collect(Collectors.toList()));

      String json = objectMapper.writeValueAsString(payload);
      EventoLogistica evento = new EventoLogistica("INICIO_RUTA", json);
      repositorioEventos.save(evento);

    } catch (Exception e) {
      System.err.println("Error serializando evento de inicio de ruta: " + e.getMessage());
    }
  }

  public void publicarEntregaConfirmada(ItemEntrega item) {
    try {
      Map<String, Object> payload = new HashMap<>();
      payload.put("idDonacion", item.getIdDonacion());
      payload.put("idEntidad", item.getEntidadDestino().getIdEntidadBeneficiaria());

      String json = objectMapper.writeValueAsString(payload);
      EventoLogistica evento = new EventoLogistica("ENTREGA_CONFIRMADA", json);
      repositorioEventos.save(evento);

    } catch (Exception e) {
      System.err.println("Error serializando evento de entrega: " + e.getMessage());
    }
  }
}