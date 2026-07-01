package ar.edu.utn.frba.ddsi.donaciones.clients;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.InfoEntregasDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.InfoRutasDTO;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LogisticaClient {
    private final RestTemplate restTemplate;

    //:http://localhost:8001/logistica
    @Value("${servicio.logisticas.url}")
    private String logisticaUrl;

    public LogisticaClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public InfoRutasDTO recibirInfoCreacionRutas(InfoEntregasDTO dto){
        return restTemplate.postForEntity(
                logisticaUrl,
                dto,
                InfoRutasDTO.class
        ).getBody();
    }
}
