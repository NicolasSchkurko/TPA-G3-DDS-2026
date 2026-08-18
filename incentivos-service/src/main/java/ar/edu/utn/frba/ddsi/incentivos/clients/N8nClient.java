package ar.edu.utn.frba.ddsi.incentivos.clients;

import ar.edu.utn.frba.ddsi.incentivos.dto.n8n.PerfilPublicacionDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class N8nClient {
    // cliente para consumir n8n y publicar cuando perfil gana una insignia
    @Value("${servicio.n8n.url}")
    private String n8nUrl;

    private final RestTemplate restTemplate;

    public N8nClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void publicarInsignia(String nombreInsignia,
                                 String nombreMision,
                                 String nombreUsuario){
        PerfilPublicacionDTO publicar = new PerfilPublicacionDTO(
                nombreUsuario,
                nombreInsignia,
                nombreMision
        );
        restTemplate.postForEntity(
                n8nUrl,
                publicar,
                void.class
        );
    }
}