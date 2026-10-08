package ar.edu.utn.frba.ddsi.donaciones.RabbitMQ;

import ar.edu.utn.frba.ddsi.donaciones.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorEventosLogistica;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioAdministradores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Recibe los eventos de trazabilidad de logística y dispara las notificaciones: es el
 * service worker del public/subscribe del enunciado (logística publica el hecho y no llama
 * a nadie; nadie conoce los donantes como este servicio).
 *
 * <p>Consume un evento por mensaje (una lista desalinearía el contrato con el productor),
 * el cursor es de este listener (quien consume sabe hasta dónde llegó) y un evento repetido
 * no se vuelve a notificar (el broker puede redeliverar).
 */
@Component
public class EventosListener {

    private static final Logger log = LoggerFactory.getLogger(EventosListener.class);

    /** Ultimo id procesado. Arranca en 0 porque los ids son 1-based. */
    private long ultimoIdProcesado = 0L;

    private final RepositorioAdministradores repoAdministradores;
    private final GestorEventosLogistica gestorLogistica;

    public EventosListener(
            RepositorioAdministradores repoAdministradores,
            GestorEventosLogistica gestorLogistica
    ) {
        this.repoAdministradores = repoAdministradores;
        this.gestorLogistica = gestorLogistica;
    }

    @RabbitListener(queues = RabbitMQConfig.COLA_EVENTOS)
    public void recibirEvento(EventoLogisticaDTO evento) {
        if (evento == null || evento.getId() == null) {
            log.warn("LLEGA un evento sin id, se descarta");
            return;
        }

        // Redelivery: el broker puede reenviar el mismo evento si el consumidor se reinicio
        // antes de confirmar. Notificar dos veces el mismo evento es visible para el donante.
        if (evento.getId() <= ultimoIdProcesado) {
            log.debug("El evento {} ya estaba procesado, se ignora", evento.getId());
            return;
        }

        List<MedioDeContacto> contactosAdmins = repoAdministradores.obtenerTodos()
                .stream()
                .map(Administrador::getContacto)
                .collect(Collectors.toList());

        try {
            gestorLogistica.procesarEvento(evento, contactosAdmins);
            ultimoIdProcesado = evento.getId();
            log.info("Evento {} procesado", evento.getId());
        } catch (RuntimeException error) {
            // Se aísla: si el error saliera del listener, el broker reintentaría el mismo
            // mensaje para siempre y la cola quedaría trabada.
            log.error("No se pudo procesar el evento {}: {}",
                    evento.getId(), error.getMessage(), error);
        }
    }

    /** Para tests y para diagnóstico del cursor. */
    public long getUltimoIdProcesado() {
        return ultimoIdProcesado;
    }
}