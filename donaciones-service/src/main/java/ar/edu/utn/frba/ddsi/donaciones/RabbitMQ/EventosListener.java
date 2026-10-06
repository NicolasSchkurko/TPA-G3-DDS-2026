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
 * Recibe los eventos de trazabilidad de logistica y dispara las notificaciones.
 *
 * <p><b>Es el service worker del public/subscribe del enunciado.</b> Logistica publica el
 * hecho y no llama a nadie: el enunciado prohibe explicitamente que invoque a este servicio o
 * a incentivos, y tambien que hable con el de notificaciones. La notificacion de "inicio de
 * ruta", "entrega realizada" y "entrega no satisfactoria" sale de aca, que es el servicio que
 * conoce a los donantes, las entidades y los administradores.
 *
 * <p><b>Consume un evento por mensaje, no una lista.</b> El productor publica un
 * {@code EventoLogisticaDTO} suelto por mensaje. Declarar la lista hacia que el mensaje llega al
 * listener y falla con "Failed to convert message", que es la version menos descriptiva de
 * este error porque no dice que hay un desajuste de contrato.
 *
 * <p><b>El cursor es de este listener.</b> Antes tomaba el ultimo id del
 * {@code LogisticaPollingScheduler} y se lo devolvia modificado, con lo que el estado de la
 * sincronizacion quedaba repartido entre dos beans y dependia del orden de ejecucion. Ahora es
 * de quien consume, que es quien sabe hasta donde llego.
 *
 * <p><b>Un evento repetido no se vuelve a notificar.</b> El broker puede redeliverar si el
 * consumidor cae a mitad del procesamiento, y sin este filtro el donante recibiria dos avisos
 * del mismo evento.
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