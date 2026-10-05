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
 * Publica en n8n cuando un donante gana una insignia, para que se comparta en redes.
 *
 * <p>n8n es un flujo externo al que no le podemos exigir disponibilidad: si está caído, la
 * insignia ya está guardada y hay que avisar igual. Por eso este listener no relanza.
 */
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

    /**
     * Publica en n8n que el donante ganó una insignia.
     *
     * <p><b>No relanza.</b> Este listener corre dentro del {@code afterCommit} de la
     * transacción, que Spring invoca sin try/catch
     * ({@code TransactionSynchronizationUtils.invokeAfterCommit}): si la excepción sale de
     * acá, sube por el {@code processCommit}, sale del {@code @Transactional} y llega al
     * handler HTTP. O sea que el donante recibía un 500 **aunque la transacción ya se
     * hubiera confirmado**, la donación estuviera guardada y la insignia otorgada. Con ese
     * 500, {@code donaciones-service} reintenta y la segunda pasada vuelve a sumar
     * progreso, que es el punto 14.
     *
     * <p>Por eso la falla se registra y queda en pendientes para reintentar, en vez de
     * propagarse. Es la misma asimetría que ya estaba resuelta en
     * {@link NotificacionClient}, cuyo helper privado captura la excepción y solo loguea.
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
