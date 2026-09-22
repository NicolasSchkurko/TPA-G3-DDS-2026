package ar.edu.utn.frba.ddsi.donaciones.models.sheduler;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaResponseDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorEventosLogistica;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioAdministradores;
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

    private final GestorEventosLogistica gestorLogistica;
    private final RepositorioAdministradores repositorioAdministradores;
    private final RestTemplate restTemplate;

    @Value("${servicio.logisticas.url}")
    private String logisticasUrl;

    private Long ultimoIdProcesado = 0L;

    public LogisticaPollingScheduler(GestorEventosLogistica gestorLogistica, RepositorioAdministradores repositorioAdministradores, RestTemplate restTemplate) {
        this.gestorLogistica = gestorLogistica;
        this.repositorioAdministradores = repositorioAdministradores;
        this.restTemplate = restTemplate;
    }

    // Se ejecuta cada 2 minutos (120000 ms)
    @Scheduled(fixedDelay = 120000)
    public void buscarNuevosEventosLogistica() {
        System.out.println("[Polling] Buscando nuevos eventos de Logística vía HTTP...");
        try {
            // Limpiamos la URL y armamos el endpoint de forma segura
            String baseUrl = logisticasUrl.endsWith("/") ? logisticasUrl.substring(0, logisticasUrl.length() - 1) : logisticasUrl;
            String urlCompleta = baseUrl + "/eventos?desdeId=" + ultimoIdProcesado; //El /api ya se le asigna en el docker compose

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
                    List<MedioDeContacto> contactosAdmins = repositorioAdministradores.obtenerTodos()
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
        } catch (Exception e) {
            System.err.println("Error al consumir eventos de Logística por Polling: " + e.getMessage());
        }
    }
}