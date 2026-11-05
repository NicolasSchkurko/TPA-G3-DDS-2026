package ar.edu.utn.frba.ddsi.donaciones.RabbitMQ;

import ar.edu.utn.frba.ddsi.donaciones.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.donaciones.exceptions.EventoYaProcesadoException;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorEventosLogistica;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioAdministradores;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioEventosProcesados;
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
 * <p>Consume un evento por mensaje (una lista desalinearía el contrato con el productor).
 * La deduplicación vive en la tabla {@code eventos_logistica_procesados} (una fila por
 * evento con éxito, clave primaria {@code id_evento}): un evento repetido no se vuelve a
 * notificar (el broker puede redeliverar), el estado sobrevive a los reinicios del
 * servicio y es compartido entre todas las réplicas del clúster.
 *
 * <p>La verificación y la inserción son atómicas: {@link GestorEventosLogistica#procesarEvento}
 * reclama la fila al comienzo de su propia transacción y la inserta antes de procesar. Si
 * el evento falla, el rollback borra el reclamo y el retry lo reprocesa; si otra réplica
 * ya lo había reclamado, la violación de clave primaria llega como
 * {@link EventoYaProcesadoException} y este listener confirma el mensaje sin reprocesar.
 *
 * <p>El procesamiento corre dentro de una transacción: en este hilo no hay OSIV, así que sin
 * ella las donaciones llegan detached y el primer acceso a una colección lazy
 * ({@code historialEstados}, {@code bienes}, {@code subcategoria}) revienta con
 * {@code LazyInitializationException}.
 */
@Component
public class EventosListener {

    private static final Logger log = LoggerFactory.getLogger(EventosListener.class);

    private final RepositorioAdministradores repoAdministradores;
    private final RepositorioEventosProcesados repoEventosProcesados;
    private final GestorEventosLogistica gestorLogistica;

    public EventosListener(
            RepositorioAdministradores repoAdministradores,
            RepositorioEventosProcesados repoEventosProcesados,
            GestorEventosLogistica gestorLogistica
    ) {
        this.repoAdministradores = repoAdministradores;
        this.repoEventosProcesados = repoEventosProcesados;
        this.gestorLogistica = gestorLogistica;
    }

    @RabbitListener(queues = RabbitMQConfig.COLA_EVENTOS)
    public void recibirEvento(EventoLogisticaDTO evento) {
        if (evento == null || evento.getId() == null) {
            log.warn("LLEGA un evento sin id, se descarta");
            return;
        }

        long idEvento = evento.getId();

        // Camino rápido: redelivery de un evento ya confirmado. El reclamo atómico dentro
        // de procesarEvento cubre la carrera entre réplicas; esto solo evita abrir una
        // transacción (y consultar administradores) para un evento que ya sabemos visto.
        if (repoEventosProcesados.existe(idEvento)) {
            log.debug("El evento {} ya estaba procesado, se ignora", idEvento);
            return;
        }

        List<MedioDeContacto> contactosAdmins = repoAdministradores.obtenerTodos()
                .stream()
                .map(Administrador::getContacto)
                .collect(Collectors.toList());

        try {
            gestorLogistica.procesarEvento(evento, contactosAdmins);
        } catch (EventoYaProcesadoException yaProcesado) {
            // Otra réplica (o una entrega anterior) reclamó el evento antes que nosotros:
            // confirmar el mensaje evita que el retry lo procese otra vez.
            log.info("El evento {} ya fue procesado por otra instancia, se ignora", idEvento);
            return;
        } catch (RuntimeException error) {
            // El reclamo se hizo dentro de la transacción del gestor y el rollback lo
            // deshizo: la próxima entrega puede reprocesar el evento. La excepción se
            // propaga y el mecanismo de reintentos la vuelve a intentar (hasta el límite
            // configurado); si agota los intentos el broker la descarta sin reenviar.
            log.error("No se pudo procesar el evento {}: {}",
                    idEvento, error.getMessage(), error);
            throw error;
        }

        log.info("Evento {} procesado", idEvento);
    }
}
