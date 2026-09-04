package ar.edu.utn.frba.ddsi.incentivos.clients;

import ar.edu.utn.frba.ddsi.incentivos.dto.n8n.PerfilPublicacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPublicacionesPendientes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class N8nClient {
    // cliente para consumir n8n y publicar cuando perfil gana una insignia
    @Value("${servicio.n8n.url}")
    private String n8nUrl;
    private final RepositorioPublicacionesPendientes repositorio;
    private final RestTemplate restTemplate;

    public N8nClient(RestTemplate restTemplate,
                     RepositorioPublicacionesPendientes repositorio) {
        this.restTemplate = restTemplate;
        this.repositorio = repositorio;
    }

    @EventListener
    public void publicarInsignia(MisionCambiada event){
        PerfilPublicacionDTO publicar = new PerfilPublicacionDTO(
                event.nombreUsuario(),
                event.insigniaAnterior(),
                event.misionAnterior()
        );
        restTemplate.postForEntity(
                n8nUrl,
                publicar,
                void.class
        );
    }
}