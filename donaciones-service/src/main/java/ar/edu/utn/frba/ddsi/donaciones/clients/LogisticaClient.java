package ar.edu.utn.frba.ddsi.donaciones.clients;

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
    public void entregarDonaciones(EntregaDonacionDTO dto) throws EntregarDonacionesException {
        try {
            restTemplate.postForEntity(
                    logisticaUrl,
                    dto,
                    void.class
            );
        } catch (Exception e) {
            throw new EntregarDonacionesException(dto);
        }
    }
}
