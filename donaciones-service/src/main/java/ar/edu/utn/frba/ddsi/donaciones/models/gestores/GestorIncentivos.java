package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IDDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IncentivosDonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.incentivos.NotificacionIncentivosPendiente;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.incentivos.TipoNotificacionIncentivos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeNotificacionesIncentivosPendientes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Coordina la outbox de notificaciones hacia incentivos (punto 76): encola en la transacción
 * del llamador, hace un intento inmediato post-commit y reintenta lo pendiente con lease y
 * backoff. El HTTP vive en {@link IncentivosClient}; acá queda la orquestación.
 */
@Service
public class GestorIncentivos {

    private static final Logger log = LoggerFactory.getLogger(GestorIncentivos.class);
    private static final long LEASE_SEGUNDOS = 60;
    private static final long RETRASO_BASE_SEGUNDOS = 30;
    private static final long RETRASO_MAXIMO_SEGUNDOS = 3600;
    private static final int MAX_POR_CICLO = 20;

    private final RepositorioDeNotificacionesIncentivosPendientes repositorio;
    private final IncentivosClient incentivosClient;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate requiereNuevaTransaccion;

    public GestorIncentivos(RepositorioDeNotificacionesIncentivosPendientes repositorio,
                            IncentivosClient incentivosClient,
                            ObjectMapper objectMapper,
                            PlatformTransactionManager transactionManager) {
        this.repositorio = repositorio;
        this.incentivosClient = incentivosClient;
        this.objectMapper = objectMapper;
        this.requiereNuevaTransaccion = new TransactionTemplate(transactionManager);
        this.requiereNuevaTransaccion.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Encola el impacto de una entrega en la transacción que la confirma. */
    @Transactional
    public UUID encolarImpacto(UUID idUsuario, IncentivosDonacionDTO dto) {
        return encolar(TipoNotificacionIncentivos.IMPACTO_DONACION, idUsuario, dto);
    }

    /** Encola la actualización de perfil en la transacción que modifica al donante. */
    @Transactional
    public UUID encolarActualizacionPerfil(UUID idUsuario, IDDTO dto) {
        return encolar(TipoNotificacionIncentivos.ACTUALIZACION_PERFIL, idUsuario, dto);
    }

    /** Encola la baja del perfil en la transacción que da de baja al donante. */
    @Transactional
    public UUID encolarEliminacionPerfil(UUID idUsuario) {
        return encolar(TipoNotificacionIncentivos.ELIMINAR_PERFIL, idUsuario, Map.of());
    }

    private UUID encolar(TipoNotificacionIncentivos tipo, UUID idUsuario, Object payload) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la notificación a incentivos", e);
        }
        NotificacionIncentivosPendiente pendiente =
                new NotificacionIncentivosPendiente(tipo, idUsuario, payloadJson, LocalDateTime.now());
        repositorio.save(pendiente);
        return pendiente.getId();
    }

    /** Intenta enviar una notificación puntual sin propagar la falla (ya quedó persistida). */
    public void intentarAhora(UUID id) {
        try {
            NotificacionReclamada reclamada = requiereNuevaTransaccion.execute(
                    estado -> reclamarPorId(id, LocalDateTime.now()));
            if (reclamada != null) {
                enviar(reclamada);
            }
        } catch (Exception e) {
            log.warn("No se pudo procesar el intento inmediato de la notificación {}: {}",
                    id, e.getMessage());
        }
    }

    /**
     * Reintenta lo que quedó pendiente. Un lease vencido permite recuperar el trabajo si
     * el proceso se cayó entre el reclamo y el envío.
     */
    @Scheduled(
            fixedDelayString = "${servicio.incentivos.outbox.intervalo-reintento-ms:30000}",
            initialDelayString = "${servicio.incentivos.outbox.retraso-inicial-ms:10000}"
    )
    public void procesarPendientes() {
        for (int procesadas = 0; procesadas < MAX_POR_CICLO; procesadas++) {
            NotificacionReclamada reclamada = requiereNuevaTransaccion.execute(
                    estado -> reclamarSiguiente(LocalDateTime.now()));
            if (reclamada == null) {
                return;
            }
            enviar(reclamada);
        }
    }

    private NotificacionReclamada reclamarSiguiente(LocalDateTime ahora) {
        return repositorio.buscarDisponibles(ahora, PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(pendiente -> reclamar(pendiente, ahora))
                .orElse(null);
    }

    private NotificacionReclamada reclamarPorId(UUID id, LocalDateTime ahora) {
        return repositorio.buscarPorIdDisponible(id, ahora)
                .map(pendiente -> reclamar(pendiente, ahora))
                .orElse(null);
    }

    private NotificacionReclamada reclamar(NotificacionIncentivosPendiente pendiente,
                                           LocalDateTime ahora) {
        pendiente.reclamarHasta(ahora.plusSeconds(LEASE_SEGUNDOS));
        return new NotificacionReclamada(
                pendiente.getId(),
                pendiente.getTipo(),
                pendiente.getIdUsuario(),
                pendiente.getPayloadJson(),
                pendiente.getIntentos());
    }

    private void enviar(NotificacionReclamada reclamada) {
        try {
            switch (reclamada.tipo()) {
                case IMPACTO_DONACION -> incentivosClient.notificarImpactoDonacion(
                        reclamada.idUsuario(),
                        objectMapper.readValue(reclamada.payloadJson(), IncentivosDonacionDTO.class));
                case ACTUALIZACION_PERFIL -> incentivosClient.actualizarPerfil(
                        reclamada.idUsuario(),
                        objectMapper.readValue(reclamada.payloadJson(), IDDTO.class));
                case ELIMINAR_PERFIL -> incentivosClient.eliminarPerfil(
                        reclamada.idUsuario());
                default -> throw new IllegalStateException(
                        "Tipo de notificación a incentivos desconocido: " + reclamada.tipo());
            }
            requiereNuevaTransaccion.executeWithoutResult(
                    estado -> repositorio.deleteById(reclamada.id()));
            log.info("Notificación {} a incentivos confirmada en el intento {}",
                    reclamada.tipo(), reclamada.intentos());
        } catch (Exception e) {
            requiereNuevaTransaccion.executeWithoutResult(
                    estado -> reintentar(reclamada.id(), LocalDateTime.now(), e.getMessage()));
            log.warn("No se pudo notificar {} a incentivos en el intento {}; se reintentará: {}",
                    reclamada.tipo(), reclamada.intentos(), e.getMessage());
        }
    }

    private void reintentar(UUID id, LocalDateTime ahora, String error) {
        repositorio.findById(id).ifPresent(pendiente -> {
            int exponente = Math.min(Math.max(pendiente.getIntentos() - 1, 0), 7);
            long espera = Math.min(RETRASO_BASE_SEGUNDOS * (1L << exponente),
                    RETRASO_MAXIMO_SEGUNDOS);
            pendiente.reintentarEn(ahora.plusSeconds(espera), error);
        });
    }

    /** Fila reclamada, lista para enviarse fuera de la transacción de base de datos. */
    public record NotificacionReclamada(UUID id,
                                        TipoNotificacionIncentivos tipo,
                                        UUID idUsuario,
                                        String payloadJson,
                                        int intentos) {
    }
}
