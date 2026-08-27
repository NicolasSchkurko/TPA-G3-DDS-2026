package ar.edu.utn.frba.ddsi.donaciones.models.sheduler;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaResponseDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorAdministradores;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorLogistica;
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

    @Value("${servicio.logisticas.url}")
    private String logisticasUrl;

    private Long ultimoIdProcesado = 0L;

    public LogisticaPollingScheduler(GestorLogistica gestorLogistica, GestorAdministradores gestorAdministradores, RestTemplate restTemplate) {
        this.gestorLogistica = gestorLogistica;
        this.gestorAdministradores = gestorAdministradores;
        this.restTemplate = restTemplate;
    }

    // Se ejecuta cada 2 minutos (120000 ms)
    @Scheduled(fixedDelay = 120000)
    public void buscarNuevosEventosLogistica() {
        System.out.println("[Polling] Buscando nuevos eventos de Logística vía HTTP...");
        try {
            // Limpiamos la URL y armamos el endpoint de forma segura
            String baseUrl = logisticasUrl.endsWith("/") ? logisticasUrl.substring(0, logisticasUrl.length() - 1) : logisticasUrl;
            String urlCompleta = baseUrl + "/eventos?desdeId=" + ultimoIdProcesado;

            // FIX: Ahora pedimos el objeto envoltorio (EventoLogisticaResponseDTO) en lugar de una Lista genérica
            ResponseEntity<EventoLogisticaResponseDTO> response = restTemplate.exchange(
                urlCompleta,
                HttpMethod.GET,
                null,
                EventoLogisticaResponseDTO.class
            );

            // Extraemos la lista del envoltorio, verificando que no venga nulo
            if (response.getBody() != null && response.getBody().getEventos() != null) {
                List<EventoLogisticaDTO> eventos = response.getBody().getEventos();

                if (!eventos.isEmpty()) {
                    List<MedioDeContacto> contactosAdmins = gestorAdministradores.listarTodosLosAdministradores()
                                                                                 .stream()
                                                                                 .map(Administrador::getContacto)
                                                                                 .collect(Collectors.toList());

                    for (EventoLogisticaDTO evento : eventos) {
                        gestorLogistica.procesarEvento(evento, contactosAdmins);
                        ultimoIdProcesado = Math.max(ultimoIdProcesado, evento.getId());
                    }

                    System.out.println("[Polling] Se procesaron " + eventos.size() + " eventos de logística.");
                } else {
                    System.out.println("[Polling] No hay eventos nuevos.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error al consumir eventos de Logística por Polling: " + e.getMessage());
        }
    }
}