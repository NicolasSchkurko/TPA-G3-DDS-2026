package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.dto.evento.PayloadEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.PayloadInicioRutaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.eventos.RepositorioEventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.messaging.ProductorEventosLogistica;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Publica los eventos de logística: alta de ruta, entrega confirmada, fallida y reingreso.
 */
@Component
public class GestorPublicacionEventos {
    private static final Logger log = LoggerFactory.getLogger(GestorPublicacionEventos.class);

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final String TEMPLATE_URL_SEGUIMIENTO = "https://donaciones-app.example.com/seguimiento/";

    private final RepositorioEventoLogistica repoEventos;
    private final ObjectMapper objectMapper;
    private final ProductorEventosLogistica productorEventos;

    public GestorPublicacionEventos(RepositorioEventoLogistica repoEventos,
                                    ObjectMapper objectMapper,
                                    ProductorEventosLogistica productorEventos) {
        this.repoEventos = repoEventos;
        this.objectMapper = objectMapper;
        this.productorEventos = productorEventos;
    }

    public Ruta publicarInicioRuta(Ruta ruta) {
        ruta.setUrlSeguimiento(TEMPLATE_URL_SEGUIMIENTO + ruta.getIdRuta());

        ruta.getParadas().forEach(parada ->
                parada.getItems().forEach(item -> {
                    if (item.getEstado() == EstadoEntrega.PENDIENTE) {
                        item.getEstado().cambiarEstado(item, EstadoEntrega.EN_CAMINO);
                    }
                })
        );

        List<String> idsDonacion = ruta.obtenerTodosLosItems().stream()
                .map(item -> item.getIdDonacion().toString())
                .toList();

        PayloadInicioRutaDTO payload = new PayloadInicioRutaDTO(idsDonacion, ruta.getUrlSeguimiento());

        EventoLogistica evento = new EventoLogistica(
                "INICIO_RUTA", ruta.getIdRuta().toString(), LocalDateTime.now(), null
        );
        evento.setPayloadJson(serializar(payload));

        repoEventos.save(evento);
        publicarAlCommit(evento);

        return ruta;
    }

    public ItemEntrega publicarEntregaConfirmada(ItemEntrega item, Ruta ruta, String foto) {
        if (item.getEstado() != EstadoEntrega.EN_CAMINO) {
            throw new IllegalStateException("No se puede confirmar la entrega: la donación "
                    + item.getIdDonacion() + " no está en camino (estado actual: " + item.getEstado() + ").");
        }

        item.setFotoComprobante(foto);
        item.getEstado().cambiarEstado(item, EstadoEntrega.ENTREGADA);

        EventoLogistica evento = new EventoLogistica(
                "ENTREGA_CONFIRMADA", item.getIdDonacion().toString(), LocalDateTime.now(), null
        );
        evento.setPayloadJson(serializar(payloadDatosEntrega(item, ruta)));

        guardarEventoDeItem(item, evento);
        return item;
    }

    public ItemEntrega publicarEntregaFallida(ItemEntrega item, Ruta ruta, String justificacion) {
        if (item.getEstado() != EstadoEntrega.EN_CAMINO) {
            throw new IllegalStateException("No se puede registrar como fallida la entrega: la donación "
                    + item.getIdDonacion() + " no está en camino (estado actual: " + item.getEstado() + ").");
        }

        item.getEstado().cambiarEstado(item, EstadoEntrega.NO_RECIBIDA);

        EventoLogistica evento = new EventoLogistica(
                "ENTREGA_FALLIDA", item.getIdDonacion().toString(), LocalDateTime.now(), justificacion
        );
        evento.setPayloadJson(serializar(payloadDatosEntrega(item, ruta)));

        guardarEventoDeItem(item, evento);

        return item;
    }

    public ItemEntrega publicarReingresoDeposito(ItemEntrega item) {
        if (item.getEstado() != EstadoEntrega.NO_RECIBIDA) {
            throw new IllegalStateException("Solo se puede reingresar a depósito una entrega en estado NO_RECIBIDA "
                    + "(estado actual: " + item.getEstado() + ").");
        }

        item.getEstado().cambiarEstado(item, EstadoEntrega.PENDIENTE);

        EventoLogistica evento = new EventoLogistica(
                "REINGRESO_DEPOSITO", item.getIdDonacion().toString(), LocalDateTime.now(), null
        );

        guardarEventoDeItem(item, evento);

        return item;
    }

    private void guardarEventoDeItem(ItemEntrega item, EventoLogistica evento) {
        evento.setItem(item);
        repoEventos.save(evento);
        publicarAlCommit(evento);
    }

    private void publicarAlCommit(EventoLogistica evento) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            productorEventos.publicar(evento);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                productorEventos.publicar(evento);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    log.warn("Transacción sin commit: el evento {} no se publica", evento.getTipoEvento());
                }
            }
        });
    }

    private PayloadEntregaDTO payloadDatosEntrega(ItemEntrega item, Ruta ruta) {
        LocalDateTime momento = item.getFechaCambioEstado();

        return new PayloadEntregaDTO(
                momento != null ? momento.format(FORMATO_FECHA) : null,
                momento != null ? momento.format(FORMATO_HORA) : null,
                ruta.getCamionAsignado() != null ? ruta.getCamionAsignado().getPatente() : null,
                this.nombreChofer(ruta)
        );
    }

    private String nombreChofer(Ruta ruta) {
        if (ruta.getCamionAsignado() == null || ruta.getCamionAsignado().getChofer() == null) {
            return null;
        }
        return ruta.getCamionAsignado().getChofer().getNombre();
    }

    private String serializar(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Error serializando payload de evento de logística: " + e.getMessage(), e);
        }
    }
}
