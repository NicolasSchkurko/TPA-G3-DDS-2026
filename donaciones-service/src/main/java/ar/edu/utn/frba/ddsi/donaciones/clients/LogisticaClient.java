package ar.edu.utn.frba.ddsi.donaciones.clients;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.InfoEntregasDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.InfoRutasDTO;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LogisticaClient {
    private final RestTemplate restTemplate;

    @Value("${servicio.logistica.url}") //TODO
    private String logisticaUrl;

    public LogisticaClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    //TODO
    public InfoRutasDTO entregarDonaciones(InfoEntregasDTO dto){
        return restTemplate.postForEntity(
                logisticaUrl,
                dto,
                InfoRutasDTO.class
        ).getBody();
    }
}
