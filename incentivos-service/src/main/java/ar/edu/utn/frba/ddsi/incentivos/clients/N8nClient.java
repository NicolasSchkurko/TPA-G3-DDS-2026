package ar.edu.utn.frba.ddsi.incentivos.clients;

import ar.edu.utn.frba.ddsi.incentivos.dto.n8n.PerfilPublicacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCompletada;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioInsignias;
import ar.edu.utn.frba.ddsi.incentivos.services.PublicacionesN8nService;
import ar.edu.utn.frba.ddsi.incentivos.services.PublicacionesN8nService.PublicacionReclamada;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** Encola y publica en n8n las insignias ganadas por los donantes. */
@Slf4j
@Service
public class N8nClient {
    private static final int MAX_PUBLICACIONES_POR_CICLO = 50;

    @Value("${servicio.n8n.url}")
    private String n8nUrl;
    private final PublicacionesN8nService publicaciones;
    private final RestTemplate restTemplate;
    private final RepositorioInsignias insignias;
    private final TaskExecutor n8nExecutor;

    @Autowired
    public N8nClient(RestTemplate restTemplate,
                     PublicacionesN8nService publicaciones,
                     RepositorioInsignias insignias,
                     @Qualifier("n8nExecutor") TaskExecutor n8nExecutor) {
        this.restTemplate = restTemplate;
        this.publicaciones = publicaciones;
        this.insignias = insignias;
        this.n8nExecutor = n8nExecutor;
    }

    /** Compatibilidad con tests y usos que no actualizan la imagen de la insignia. */
    public N8nClient(RestTemplate restTemplate,
                     PublicacionesN8nService publicaciones,
                     TaskExecutor n8nExecutor) {
        this(restTemplate, publicaciones, null, n8nExecutor);
    }

    public N8nClient(RestTemplate restTemplate, PublicacionesN8nService publicaciones) {
        this(restTemplate, publicaciones, null, null);
    }

    /**
     * Guarda el mensaje en la outbox dentro de la misma transaccion que guarda el perfil.
     * Si no se puede persistir, la transaccion de la insignia falla en vez de confirmar una
     * actualizacion que no podria publicarse luego.
     *
     * @param event mision completada que genero una insignia
     */
    @EventListener
    public void encolarInsignia(MisionCompletada event) {
        PerfilPublicacionDTO publicar = new PerfilPublicacionDTO(
            "en el centro debe decir " + event.insigniaObtenida(),
            event.nombreUsuario() + " ganó la insignia " + event.insigniaObtenida()
                + " tras haber completado la misión " + event.misionAnterior(),
            "discord",
            event.nombreUsuario(),
            event.idUsuario(),
            event.idInsignia()
        );
        publicaciones.encolar(publicar);
    }

    /**
     * Envía filas reclamadas fuera de la transaccion de persistencia. Un lease vencido
     * permite recuperar el trabajo tras una caida del proceso.
     */
    @Scheduled(
            fixedDelayString = "${servicio.n8n.intervalo-reintento-ms:30000}",
            initialDelayString = "${servicio.n8n.retraso-inicial-ms:5000}"
    )
    public void procesarPendientes() {
        for (int procesadas = 0; procesadas < MAX_PUBLICACIONES_POR_CICLO; procesadas++) {
            Optional<PublicacionReclamada> siguiente =
                    publicaciones.reclamarSiguiente(LocalDateTime.now());
            if (siguiente.isEmpty()) {
                return;
            }

            PublicacionReclamada reclamada = siguiente.orElseThrow();
            if (n8nExecutor == null) {
                enviar(reclamada);
            } else {
                n8nExecutor.execute(() -> enviar(reclamada));
            }
        }
    }

    private void enviar(PublicacionReclamada reclamada) {
        try {
            var respuesta = restTemplate.postForEntity(
                    n8nUrl, reclamada.contenido(), Map.class);
            Object valorUrl = respuesta.getBody() == null
                    ? null : respuesta.getBody().get("urlImagen");
            if (reclamada.contenido().getIdInsignia() != null) {
                if (!(valorUrl instanceof String urlImagen) || urlImagen.isBlank()) {
                    throw new IllegalStateException(
                            "n8n no devolvio una urlImagen para la insignia generada");
                }
                if (insignias != null) {
                    Insignia insignia = insignias.findById(
                                    reclamada.contenido().getIdInsignia())
                            .orElseThrow(() -> new IllegalStateException(
                                    "No existe la insignia "
                                            + reclamada.contenido().getIdInsignia()));
                    insignia.actualizar(null, null, urlImagen);
                    insignias.save(insignia);
                }
            }
            publicaciones.confirmar(reclamada.id());
            log.info("Publicacion {} enviada a {} en el intento {}",
                    reclamada.id(), reclamada.contenido().getRedSocial(),
                    reclamada.intentos());
        } catch (RestClientException | IllegalStateException exception) {
            publicaciones.reintentar(reclamada.id(), LocalDateTime.now(),
                    exception.getMessage());
            log.warn("No se pudo enviar la publicacion {} en el intento {}; se reintentara",
                    reclamada.id(), reclamada.intentos(), exception);
        }
    }
}
