package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.dto.evento.PayloadEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.PayloadInicioRutaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.eventos.RepositorioEventoLogistica;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class GestorEventos {
    private final RepositorioEventoLogistica repoEventos;

    public GestorEventos(RepositorioEventoLogistica repoEventos) {
        this.repoEventos = repoEventos;
    }

    public List<EventoLogistica> buscarEventos(Long desdeId) {
        return repoEventos.findByIdGreaterThanOrderByFechaAsc(desdeId);
    }

    public void guardarEvento(EventoLogistica evento) {
        repoEventos.save(evento);
    }
}
