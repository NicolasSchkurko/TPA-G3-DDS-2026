package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.PayloadEntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.PayloadInicioRutaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.DatosMensajes.NotificacionEntregaDatos;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.DatosMensajes.NotificacionEntregaFallidaDatos;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.DatosMensajes.NotificacionViajeDatos;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiaNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDonaciones;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GestorLogistica {
    private RepositorioDonaciones repositorioDonaciones;
    private FabricaEstrategiasNotificacion fabricaEstrategias;
    private GestorAdministradores gestorAdministradores;
    private ObjectMapper objectMapper;

    public void procesarEvento(EventoLogisticaDTO evento) {
        switch (evento.getTipoEvento()) {
            case "INICIO_RUTA":
                manejarInicioRuta(evento);
                break;
            case "ENTREGA_CONFIRMADA":
                manejarEntregaConfirmada(evento);
                break;
            case "ENTREGA_FALLIDA":
                manejarEntregaFallida(evento);
                break;
            case "REINGRESO_DEPOSITO":
                manejarReingresoDeposito(evento);
                break;
            default:
                System.out.println("Evento de logística desconocido: " + evento.getTipoEvento());
        }
    }

    private void manejarInicioRuta(EventoLogisticaDTO evento) {
        try {
            if (evento.getPayloadJson() == null || evento.getPayloadJson().isEmpty()) {
                System.err.println("Evento INICIO_RUTA " + evento.getId() + " sin payload, se ignora.");
                return;
            }

            PayloadInicioRutaDTO payload =
                    objectMapper.readValue(evento.getPayloadJson(), PayloadInicioRutaDTO.class);

            if (payload.getItems() == null) {
                return;
            }

            EstrategiaNotificacion estrategiaViaje =
                    fabricaEstrategias.obtenerEstrategia(
                            TipoEventoNotificacion.DONACION_EN_VIAJE);

            for (String idTexto : payload.getItems()) {

                UUID idDonacion = UUID.fromString(idTexto);

                repositorioDonaciones.findById(idDonacion)
                        .ifPresent(donacion -> {

                            donacion.actualizarEstado(
                                    Estado.EN_TRASLADO,
                                    "Ruta iniciada por Logística");

                            repositorioDonaciones.save(donacion);

                            estrategiaViaje.ejecutar(
                                    new NotificacionViajeDatos(
                                            payload.getUrlRuta(),
                                            donacion.getDonante().getPersona().getMediosDeContacto(),
                                            donacion.getEntidad().getPersonaJuridica().getMediosDeContacto()
                                    ));
                        });
            }

        } catch (Exception e) {
            System.err.println("Error parseando items de la ruta: " + e.getMessage());
        }
    }

    private void manejarEntregaConfirmada(EventoLogisticaDTO evento) {

        UUID idDonacion = UUID.fromString(evento.getReferenciaId());

        repositorioDonaciones.findById(idDonacion)
                .ifPresent(donacion -> {

                    donacion.actualizarEstado(
                            Estado.ENTREGADO,
                            "Entrega confirmada por la entidad");

                    repositorioDonaciones.save(donacion);

                    PayloadEntregaDTO payload = parsearPayloadEntrega(evento);

                    fabricaEstrategias
                            .obtenerEstrategia(TipoEventoNotificacion.COMPROBANTE_ENTREGA)
                            .ejecutar(
                                    new NotificacionEntregaDatos(
                                            payload,
                                            donacion.getDonante().getPersona().getMediosDeContacto(),
                                            donacion.getEntidad().getPersonaJuridica().getMediosDeContacto()
                                    ));
                });
    }

    private void manejarEntregaFallida(EventoLogisticaDTO evento) {

        UUID idDonacion = UUID.fromString(evento.getReferenciaId());

        repositorioDonaciones.findById(idDonacion)
                .ifPresent(donacion -> {

                    String justificacion =
                            evento.getJustificacion() != null
                                    ? evento.getJustificacion()
                                    : "Falla reportada en destino";

                    donacion.actualizarEstado(
                            Estado.EN_DEPOSITO,
                            justificacion);

                    repositorioDonaciones.save(donacion);

                    PayloadEntregaDTO payload =
                            parsearPayloadEntrega(evento);

                    fabricaEstrategias
                            .obtenerEstrategia(TipoEventoNotificacion.ENTREGA_NO_RECIBIDA)
                            .ejecutar(
                                    new NotificacionEntregaFallidaDatos(
                                            payload,
                                            donacion.getDonante().getPersona().getMediosDeContacto(),
                                            donacion.getEntidad().getPersonaJuridica().getMediosDeContacto(),
                                            obtenerContactosAdministradores()
                                    ));
                });
    }

    private void manejarReingresoDeposito(EventoLogisticaDTO evento) {
        UUID idDonacion = UUID.fromString(evento.getReferenciaId());
        Optional<Donacion> donacionOpt = repositorioDonaciones.findById(idDonacion);

        if (donacionOpt.isPresent()) {
            Donacion donacion = donacionOpt.get();
            donacion.actualizarEstado(Estado.PENDIENTE_ASIGNACION, "Reingresado a depósito tras revisión de entrega fallida");
            repositorioDonaciones.save(donacion);
        }
    }

    private PayloadEntregaDTO parsearPayloadEntrega(EventoLogisticaDTO evento) {
        try {
            return objectMapper.readValue(evento.getPayloadJson(), PayloadEntregaDTO.class);
        } catch (Exception e) {
            System.err.println("Error parseando datos de entrega del evento " + evento.getId() + ": " + e.getMessage());
            return new PayloadEntregaDTO();
        }
    }

    public List<MedioDeContacto> obtenerContactosAdministradores() {
        return gestorAdministradores.listarTodosLosAdministradores().stream()
                .map(Administrador::getContacto)
                .toList();
    }
}
