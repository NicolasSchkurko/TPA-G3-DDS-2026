package ar.edu.utn.frba.ddsi.incentivos.clients;

import ar.edu.utn.frba.ddsi.incentivos.dto.n8n.PerfilPublicacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCompletada;
import ar.edu.utn.frba.ddsi.incentivos.services.PublicacionesN8nService;
import ar.edu.utn.frba.ddsi.incentivos.services.PublicacionesN8nService.PublicacionReclamada;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
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
    private final TaskExecutor n8nExecutor;

    public N8nClient(RestTemplate restTemplate,
                     PublicacionesN8nService publicaciones,
                     @Qualifier("n8nExecutor") TaskExecutor n8nExecutor) {
        this.restTemplate = restTemplate;
        this.publicaciones = publicaciones;
        this.n8nExecutor = n8nExecutor;
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
            event.nombreUsuario(),
            event.idUsuario()
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
            n8nExecutor.execute(() -> enviar(reclamada));
        }
    }

    private void enviar(PublicacionReclamada reclamada) {
        try {
            restTemplate.postForEntity(n8nUrl, reclamada.contenido(), void.class);
            publicaciones.confirmar(reclamada.id());
            log.info("Publicacion {} enviada a n8n en el intento {}",
                    reclamada.id(), reclamada.intentos());
        } catch (RestClientException exception) {
            publicaciones.reintentar(reclamada.id(), LocalDateTime.now(),
                    exception.getMessage());
            log.warn("No se pudo enviar la publicacion {} en el intento {}; se reintentara",
                    reclamada.id(), reclamada.intentos(), exception);
        }
    }
}
