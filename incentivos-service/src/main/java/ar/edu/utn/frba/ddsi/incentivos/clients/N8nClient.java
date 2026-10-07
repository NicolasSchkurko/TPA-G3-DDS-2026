package ar.edu.utn.frba.ddsi.incentivos.clients;

import ar.edu.utn.frba.ddsi.incentivos.dto.n8n.PerfilPublicacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCompletada;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPublicacionesPendientes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestTemplate;

/**
 * Publica en n8n cuando un donante gana una insignia, para compartirla en redes. No relanza si
 * n8n está caído: la insignia ya está guardada.
 */
@Slf4j
@Service
public class N8nClient {
    @Value("${servicio.n8n.url}")
    private String n8nUrl;
    private final RepositorioPublicacionesPendientes repositorio;
    private final RestTemplate restTemplate;

    public N8nClient(RestTemplate restTemplate,
                     RepositorioPublicacionesPendientes repositorio) {
        this.restTemplate = restTemplate;
        this.repositorio = repositorio;
    }

    /**
     * Publica en n8n que el donante ganó una insignia. No relanza: si el listener dejara
     * salir la excepción, el donante vería un 500 con la transacción ya confirmada. La falla
     * se registra y queda en pendientes.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicarInsignia(MisionCompletada event) {
        PerfilPublicacionDTO publicar = new PerfilPublicacionDTO(
            "en el centro debe decir " + event.insigniaObtenida(),
            ", por ganar la insignia " + event.insigniaObtenida()
                + " tras haber completado la mision " + event.misionAnterior(),
                "discord",
                event.nombreUsuario(),
                event.idUsuario()
        );

        try {
            restTemplate.postForEntity(n8nUrl, publicar, void.class);
            log.info("Publicacion exitosa en {}",
                    publicar.getRedSocial());
        } catch (Exception e) {
            log.error("Error al publicar en {}, queda en pendientes para reintentar",
                    publicar.getRedSocial(), e);
            repositorio.guardar(publicar);
        }
    }
}
