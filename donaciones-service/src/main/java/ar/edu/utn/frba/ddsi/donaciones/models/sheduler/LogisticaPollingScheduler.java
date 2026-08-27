package ar.edu.utn.frba.ddsi.donaciones.models.sheduler;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorAdministradores;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorLogistica;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Component
public class LogisticaPollingScheduler {

    private final GestorLogistica gestorLogistica;
    private final GestorAdministradores gestorAdministradores;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${servicio.logisticas.url}")
    private String logisticasUrl;

    private Long ultimoIdProcesado = 0L;

    public LogisticaPollingScheduler(GestorLogistica gestorLogistica, GestorAdministradores gestorAdministradores) {
        this.gestorLogistica = gestorLogistica;
        this.gestorAdministradores = gestorAdministradores;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Scheduled(fixedDelay = 120000)
    public void buscarNuevosEventosLogistica() {
        System.out.println("[Polling] Buscando nuevos eventos de Logística...");
        try {
            String urlCompleta = logisticasUrl + (logisticasUrl.endsWith("/") ? "" : "/")
                    + "eventos?desdeId=" + ultimoIdProcesado;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlCompleta))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<EventoLogisticaDTO> eventos = objectMapper.readValue(
                        response.body(),
                        new TypeReference<List<EventoLogisticaDTO>>() {}
                );

                if (!eventos.isEmpty()) {
                    List<MedioDeContacto> contactosAdmins = gestorAdministradores.listarTodosLosAdministradores()
                            .stream()
                            .map(Administrador::getContacto)
                            .toList();

                    for (EventoLogisticaDTO evento : eventos) {
                        gestorLogistica.procesarEvento(evento, contactosAdmins);
                        ultimoIdProcesado = Math.max(ultimoIdProcesado, evento.getId());
                    }
                }
            } else {
                System.err.println("Logísticas respondió con status: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Error al consumir eventos de Logística: " + e.getMessage());
        }
    }
}