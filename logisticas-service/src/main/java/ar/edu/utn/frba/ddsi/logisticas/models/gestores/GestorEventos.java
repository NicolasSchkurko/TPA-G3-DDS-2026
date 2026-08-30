package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.dto.evento.PayloadEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.PayloadInicioRutaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioEventoLogistica;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class GestorEventos {
    private final RepositorioEventoLogistica repoEventos;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final String TEMPLATE_URL_SEGUIMIENTO = "https://donaciones-app.example.com/seguimiento/";
    private final ObjectMapper objectMapper;

    public GestorEventos(RepositorioEventoLogistica repoEventos, ObjectMapper objectMapper){
        this.repoEventos = repoEventos;
        this.objectMapper = objectMapper;
    }

    public List<EventoLogistica> buscarEventos(Long desdeId) {
        return repoEventos.findByIdGreaterThanOrderByFechaAsc(desdeId);
    }

    public void guardarEvento(EventoLogistica evento){
        repoEventos.save(evento);
    }

    public Ruta publicarInicioRuta(Ruta ruta) {
        ruta.setUrlSeguimiento(TEMPLATE_URL_SEGUIMIENTO + ruta.getIdRuta());

        ruta.getParadas().forEach(parada ->
                parada.getItems().forEach(item -> {
                    if(item.getEstado() == EstadoEntrega.PENDIENTE){
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
        ruta.getParadas().forEach(parada -> parada.getItems().forEach(item -> item.getEventos().add(evento)));
        guardarEvento(evento);
        return ruta;
    }

    public ItemEntrega publicarEntregaConfirmada(ItemEntrega item, Ruta ruta, String foto) {
        if (item.getEstado() == EstadoEntrega.EN_CAMINO) {
            item.setFotoComprobante(foto);
            item.getEstado().cambiarEstado(item, EstadoEntrega.ENTREGADA);
            EventoLogistica evento = new EventoLogistica(
                    "ENTREGA_CONFIRMADA", item.getIdDonacion().toString(), LocalDateTime.now(), null
            );
            evento.setPayloadJson(serializar(payloadDatosEntrega(item, ruta)));
            item.getEventos().add(evento);
            guardarEvento(evento);
        }
        return item;
    }

    // Adaptado para recibir la justificación separada y persistirla en el evento
    public ItemEntrega publicarEntregaFallida(ItemEntrega item, Ruta ruta, String justificacion) {
        item.getEstado().cambiarEstado(item, EstadoEntrega.NO_RECIBIDA);
        EventoLogistica evento = new EventoLogistica(
                "ENTREGA_FALLIDA", item.getIdDonacion().toString(), LocalDateTime.now(), justificacion
        );
        evento.setPayloadJson(serializar(payloadDatosEntrega(item, ruta)));
        item.getEventos().add(evento);
        guardarEvento(evento);
        return item;
    }

    public ItemEntrega publicarReingresoDeposito(ItemEntrega item) {
        item.getEstado().cambiarEstado(item, EstadoEntrega.PENDIENTE);
        EventoLogistica evento = new EventoLogistica(
                "REINGRESO_DEPOSITO", item.getIdDonacion().toString(), LocalDateTime.now(), null
        );
        item.getEventos().add(evento);
        guardarEvento(evento);
        return item;
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
