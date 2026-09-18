package ar.edu.utn.frba.ddsi.logisticas.broker.adapters;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.PeticionEntregaDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component("logisticaExternaAdapter")
public class LogisticaExternaAdapter implements ServicioLogisticaAdapter {

    private final RestTemplate restTemplate;

    @Value("${logistica.externa.url:https://api-logistica-externa.com/entregas}")
    private String urlServicioExterno;

    public LogisticaExternaAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String getNombreProveedor() {
        return "EXTERNO";
    }

    @Override
    public void procesarEntrega(PeticionEntregaDTO request) {
        // Redirige el payload a la API Web del servicio externo contratado
        restTemplate.postForEntity(urlServicioExterno, request, String.class);
    }
}