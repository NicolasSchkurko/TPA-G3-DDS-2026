package ar.edu.utn.frba.ddsi.donaciones.RabbitMQ;

import ar.edu.utn.frba.ddsi.donaciones.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaResponseDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorEventosLogistica;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioAdministradores;
import ar.edu.utn.frba.ddsi.donaciones.models.sheduler.LogisticaPollingScheduler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EventosListener {
    Long ultimoIdProcesado;
    RepositorioAdministradores repoAdministradores;
    GestorEventosLogistica gestorLogistica;
    LogisticaPollingScheduler scheduler;

    public EventosListener(
            RepositorioAdministradores repoAdministradores,
            GestorEventosLogistica gestorLogistica,
            LogisticaPollingScheduler scheduler
    ) {
        this.repoAdministradores = repoAdministradores;
        this.gestorLogistica = gestorLogistica;
        this.scheduler = scheduler;
    }

    @RabbitListener(queues = RabbitMQConfig.RESPUESTA_EVENTOS_QUEUE)
    public void recibirEventos(EventoLogisticaResponseDTO eventosResponse){
        // Extraemos la lista del envoltorio, verificando que no venga nulo
        if (eventosResponse != null && eventosResponse.getEventos() != null) {
            List<EventoLogisticaDTO> eventos = eventosResponse.getEventos();

            if (!eventos.isEmpty()) {
                List<MedioDeContacto> contactosAdmins = repoAdministradores.obtenerTodos()
                        .stream()
                        .map(Administrador::getContacto)
                        .collect(Collectors.toList());

                ultimoIdProcesado = scheduler.getUltimoIdProcesado();
                for (EventoLogisticaDTO evento : eventos) {
                    gestorLogistica.procesarEvento(evento, contactosAdmins);
                    ultimoIdProcesado = Math.max(ultimoIdProcesado, evento.getId()) + 1;
                }
                scheduler.setUltimoIdProcesado(ultimoIdProcesado);

                System.out.println("[Polling] Se procesaron " + eventos.size() + " eventos de logística.");
            } else {
                System.out.println("[Polling] No hay eventos nuevos.");
            }
        }
    }
}