package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioEventoLogistica;

import java.util.List;

public class GestorEventos {
    private RepositorioEventoLogistica repoEventos;

    public List<EventoLogistica> buscarEventos(Long desdeId) {
        return repoEventos.findByIdGreaterThanOrderByFechaAsc(desdeId);
    }

    public void guardarEvento(EventoLogistica evento){
        repoEventos.save(evento);
    }
}
