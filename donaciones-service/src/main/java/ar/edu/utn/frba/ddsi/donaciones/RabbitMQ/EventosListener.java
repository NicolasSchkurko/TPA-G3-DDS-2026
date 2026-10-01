package ar.edu.utn.frba.ddsi.donaciones.RabbitMQ;

import ar.edu.utn.frba.ddsi.donaciones.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaResponseDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.util.List;
import java.util.stream.Collectors;

public class EventosListener {

    @RabbitListener(queues = RabbitMQConfig.ROUTING_KEY_RESPUESTA_EVENTOS)
    public void recibirEventos(EventoLogisticaResponseDTO eventosResponse){
        // Extraemos la lista del envoltorio, verificando que no venga nulo
        if (eventosResponse != null && eventosResponse.getEventos() != null) {
            List<EventoLogisticaDTO> eventos = eventosResponse.getEventos();

            if (!eventos.isEmpty()) {
                List<MedioDeContacto> contactosAdmins = gestorAdministradores.listarTodosLosAdministradores()
                        .stream()
                        .map(Administrador::getContacto)
                        .collect(Collectors.toList());

                for (EventoLogisticaDTO evento : eventos) {
                    gestorLogistica.procesarEvento(evento, contactosAdmins);
                    ultimoIdProcesado = Math.max(ultimoIdProcesado, evento.getId());
                    ultimoIdProcesado += 1;
                }

                System.out.println("[Polling] Se procesaron " + eventos.size() + " eventos de logística.");
            } else {
                System.out.println("[Polling] No hay eventos nuevos.");
            }
        }
    }
}
