package ar.edu.utn.frba.ddsi.donaciones.RabbitMQ;

import ar.edu.utn.frba.ddsi.donaciones.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaResponseDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorEventosLogistica;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioAdministradores;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class EventosListener {

    private final GestorEventosLogistica gestorEventosLogistica;
    private final RepositorioAdministradores repositorioAdministradores;
    private final AtomicLong siguienteId = new AtomicLong(0L);

    public EventosListener(GestorEventosLogistica gestorEventosLogistica,
                           RepositorioAdministradores repositorioAdministradores) {
        this.gestorEventosLogistica = gestorEventosLogistica;
        this.repositorioAdministradores = repositorioAdministradores;
    }

    public long getDesdeId() {
        return siguienteId.get();
    }

    @RabbitListener(queues = RabbitMQConfig.RESPUESTA_EVENTOS_QUEUE)
    public void recibirEventos(EventoLogisticaResponseDTO eventosResponse){
        if (eventosResponse != null && eventosResponse.getEventos() != null) {
            List<EventoLogisticaDTO> eventos = eventosResponse.getEventos();

            if (!eventos.isEmpty()) {
                List<MedioDeContacto> contactosAdmins = repositorioAdministradores.obtenerTodos()
                        .stream()
                        .map(Administrador::getContacto)
                        .collect(Collectors.toList());

                for (EventoLogisticaDTO evento : eventos) {
                    gestorEventosLogistica.procesarEvento(evento, contactosAdmins);
                    siguienteId.accumulateAndGet(evento.getId() + 1, Math::max);
                }

                System.out.println("[Polling] Se procesaron " + eventos.size() + " eventos de logística.");
            } else {
                System.out.println("[Polling] No hay eventos nuevos.");
            }
        }
    }
}
