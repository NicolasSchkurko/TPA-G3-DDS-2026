package ar.edu.utn.frba.ddsi.notificaciones.scheduler;

import ar.edu.utn.frba.ddsi.notificaciones.messaging.ProductorNotificaciones;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories.NotificacionReintentable;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories.RepositorioNotificaciones;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Republica los avisos de notificaciones que quedaron {@code PENDIENTE} (por ejemplo, si el
 * mensaje se perdió y nunca llegó al consumidor). No toca las {@code FALLIDA}: esas se reintentan
 * a mano por {@code POST /api/notificaciones/{id}/reintento}.
 *
 * <p>Antes de publicar, cada notificación se reclama con un compare-and-set sobre su contador de
 * intentos: solo una réplica gana el reclamo y el tope corta las repúblicaciones de una
 * {@code PENDIENTE} que nunca se consume. Si la publicación falla, el intento se devuelve.
 */
@Component
public class ReintentoNotificacionesScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReintentoNotificacionesScheduler.class);

    /**
     * Tope por corrida para acotar el heap. Las primeras 100 (por fecha) se reintentan hasta
     * agotar el tope antes de que el scheduler pase a las siguientes; el lote no drena la cola
     * entera de una vez.
     */
    private static final int TAMANO_LOTE = 100;

    private final RepositorioNotificaciones repositorioNotificaciones;
    private final ProductorNotificaciones productorNotificaciones;
    private final long antiguedadMinutos;
    private final int maxIntentos;
    private final long envioColgadoMinutos;

    public ReintentoNotificacionesScheduler(
            RepositorioNotificaciones repositorioNotificaciones,
            ProductorNotificaciones productorNotificaciones,
            @Value("${notificaciones.reintento.antiguedad-minutos:10}") long antiguedadMinutos,
            @Value("${notificaciones.reintento.max-intentos:3}") int maxIntentos,
            @Value("${notificaciones.reintento.envio-colgado-minutos:2}") long envioColgadoMinutos) {
        this.repositorioNotificaciones = repositorioNotificaciones;
        this.productorNotificaciones = productorNotificaciones;
        this.antiguedadMinutos = antiguedadMinutos;
        this.maxIntentos = maxIntentos;
        this.envioColgadoMinutos = envioColgadoMinutos;
    }

    @Scheduled(fixedDelayString = "${notificaciones.reintento.intervalo-ms:300000}",
            initialDelayString = "${notificaciones.reintento.initial-delay-ms:60000}")
    public void reintentarPendientesAntiguas() {
        int liberadas = repositorioNotificaciones.liberarEnviosColgados(
                EstadoNotificacion.PENDIENTE, EstadoNotificacion.ENVIANDO,
                LocalDateTime.now().minusMinutes(envioColgadoMinutos));
        if (liberadas > 0) {
            log.warn("[REINTENTO] Se liberaron {} notificaciones ENVIANDO colgadas", liberadas);
        }

        LocalDateTime limite = LocalDateTime.now().minusMinutes(antiguedadMinutos);
        List<NotificacionReintentable> pendientes = repositorioNotificaciones
                .findParaReintentar(EstadoNotificacion.PENDIENTE, limite, maxIntentos,
                        PageRequest.of(0, TAMANO_LOTE));

        for (NotificacionReintentable notificacion : pendientes) {
            int esperados = notificacion.intentos() == null ? 0 : notificacion.intentos();
            int reclamada = repositorioNotificaciones.reclamarParaReintento(
                    notificacion.id(), EstadoNotificacion.PENDIENTE, esperados, maxIntentos);
            if (reclamada == 0) {
                // Otra instancia la tomó o ya agotó los intentos: no se republica.
                continue;
            }

            log.info("[REINTENTO] Republicando notificación PENDIENTE {}", notificacion.id());
            try {
                productorNotificaciones.enviar(notificacion.id(),
                        notificacion.tipoMedioDeContacto(), notificacion.direccionDeContacto());
            } catch (Exception error) {
                // La publicación no salió: se devuelve el intento para no agotar el tope sin enviar.
                repositorioNotificaciones.compensarReintentoFallido(
                        notificacion.id(), EstadoNotificacion.PENDIENTE);
                log.error("[REINTENTO] No se pudo republicar la notificación {}: {}",
                        notificacion.id(), error.getMessage(), error);
            }
        }
    }
}
