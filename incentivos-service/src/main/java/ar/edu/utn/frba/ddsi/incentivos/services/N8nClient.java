package ar.edu.utn.frba.ddsi.incentivos.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class N8nClient {
    // cliente para consumir n8n y publicar cuando perfil gana una insignia
    @Value("${servicio.n8n.url}")
    private String n8nUrl;

    private final RestTemplate restTemplate;

    public N8nClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void publicarInsignia(
            String nombreUsuario,
            String nombreInsignia,
            String descripcionInsignia) {

        Map<String, String> body = new HashMap<>();
        body.put("usuario", nombreUsuario);
        body.put("insignia", nombreInsignia);
        body.put("descripcion", descripcionInsignia);

        restTemplate.postForEntity(
                n8nUrl,
                body,
                Void.class
        );
    }
}