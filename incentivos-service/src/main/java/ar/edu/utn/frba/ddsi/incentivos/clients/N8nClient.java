package ar.edu.utn.frba.ddsi.incentivos.clients;

import ar.edu.utn.frba.ddsi.incentivos.dto.n8n.PerfilPublicacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.EnvioPublicacionException;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPublicacionesPendientes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
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
    public void publicarInsignia(MisionCambiada event)
            throws EnvioPublicacionException {
        PerfilPublicacionDTO publicar = new PerfilPublicacionDTO(
                event.nombreUsuario(),
                event.insigniaAnterior(),
                event.misionAnterior()
        );
        try {
            restTemplate.postForEntity(
                    n8nUrl, publicar, void.class);
            log.info("Publicacion exitosa en {}",
                    event.contacto());
        } catch (Exception e) {
            log.error("Error al publicar en {}, guardando en pendientes",
                    event.contacto(), e);
            repositorio.guardar(publicar);
            throw new EnvioPublicacionException(publicar);
        }
    }
}