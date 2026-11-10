package ar.edu.utn.frba.ddsi.notificaciones.models.gestores;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvio;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvioFactory;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/** Elige el medio de envío y despacha la notificación. */
@Service
public class GestorEnvio {

    private static final Logger log = LoggerFactory.getLogger(GestorEnvio.class);

    private final MedioDeEnvioFactory factory;

    public GestorEnvio(MedioDeEnvioFactory factory) {
        this.factory = factory;
    }

    /** Pasa la fila a ENVIANDO y persiste medio/dirección; vacío si ya salió o está en curso. */
    public Optional<Notificacion> reclamar(Optional<Notificacion> encontrada,
                                           String medioDeContacto,
                                           String direccionDeContacto) {
        if (encontrada.isEmpty()) {
            return Optional.empty();
        }

        Notificacion notificacion = encontrada.get();
        if (notificacion.getEstado() == EstadoNotificacion.ENVIADA
                || notificacion.getEstado() == EstadoNotificacion.ENVIANDO) {
            return Optional.empty();
        }

        if (medioDeContacto != null && !medioDeContacto.isBlank()) {
            notificacion.setTipoMedioDeContacto(medioDeContacto);
        }
        if (direccionDeContacto != null && !direccionDeContacto.isBlank()) {
            notificacion.setDireccionDeContacto(direccionDeContacto);
        }
        notificacion.marcarEnviando(LocalDateTime.now());
        return Optional.of(notificacion);
    }

    public void enviarNotificacion(String tipoMedioDeContacto,
                                   String direccionContacto,
                                   Notificacion notificacion) {
        log.info("[ENVIO] Despachando notificación {} por el medio '{}' a '{}'",
                notificacion.getId(), tipoMedioDeContacto, direccionContacto);

        // La dirección del mensaje gana; los medios leen la de la entidad.
        if (direccionContacto != null && !direccionContacto.isBlank()) {
            notificacion.setDireccionDeContacto(direccionContacto);
        }

        // No se envuelve la excepción: el consumidor distingue dato inválido (IllegalArgumentException)
        // de falla de red/timeout (RestClientException) para decidir DLQ o reintento.
        MedioDeEnvio medioDeContacto = factory.mapearAMedioEnvio(tipoMedioDeContacto);
        medioDeContacto.enviarNotificacion(notificacion);

        log.info("[ENVIO] El medio '{}' resolvió el envío de la notificación {}",
                tipoMedioDeContacto, notificacion.getId());
    }
}
