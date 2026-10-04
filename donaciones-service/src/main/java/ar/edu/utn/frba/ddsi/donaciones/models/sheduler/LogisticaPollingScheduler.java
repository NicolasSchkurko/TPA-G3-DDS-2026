package ar.edu.utn.frba.ddsi.donaciones.models.sheduler;

import ar.edu.utn.frba.ddsi.donaciones.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaResponseDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.SolicitudEventosDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorAdministradores;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorLogistica;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class LogisticaPollingScheduler {

    private final GestorLogistica gestorLogistica;
    private final GestorAdministradores gestorAdministradores;
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${servicio.logisticas.url}")
    private String logisticasUrl;

    private Long ultimoIdProcesado = 0L;

    public LogisticaPollingScheduler(GestorLogistica gestorLogistica, GestorAdministradores gestorAdministradores, RestTemplate restTemplate, RabbitTemplate rabbitTemplate) {
        this.gestorLogistica = gestorLogistica;
        this.gestorAdministradores = gestorAdministradores;
        this.restTemplate = restTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    // Se ejecuta cada 2 minutos (120000 ms)
    @Scheduled(fixedDelay = 120000)
    public void buscarNuevosEventosLogistica() {
        System.out.println("[Polling] Buscando nuevos eventos de Logística vía HTTP...");
        try {
            SolicitudEventosDTO desdeId = new SolicitudEventosDTO(ultimoIdProcesado);
            rabbitTemplate.convertAndSend(RabbitMQConfig.LOGISTICAS_EXCHANGE, RabbitMQConfig.ROUTING_KEY_SOLICITUD_EVENTOS, desdeId);
        } catch (Exception e) {
            System.err.println("Error al consumir eventos de Logística por Polling: " + e.getMessage());
        }
    }
}