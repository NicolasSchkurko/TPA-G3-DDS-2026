package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IncentivosDonacionDTO;
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
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioDonaciones;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GestorEventosLogistica {
  private final RepositorioDonaciones repositorioDonaciones;
  private final FabricaEstrategiasNotificacion fabricaEstrategias;
  private final IncentivosClient incentivosClient;
  private final ObjectMapper objectMapper;

  public GestorEventosLogistica(RepositorioDonaciones repositorioDonaciones,
                                FabricaEstrategiasNotificacion fabricaEstrategias,
                                IncentivosClient incentivosClient,
                                ObjectMapper objectMapper) {
    this.repositorioDonaciones = repositorioDonaciones;
    this.fabricaEstrategias = fabricaEstrategias;
    this.incentivosClient = incentivosClient;
    this.objectMapper = objectMapper;
  }

  public void procesarEvento(EventoLogisticaDTO evento, List<MedioDeContacto> contactosAdministradores) {
    switch (evento.getTipoEvento()) {
      case "INICIO_RUTA":
        manejarInicioRuta(evento);
        break;
      case "ENTREGA_CONFIRMADA":
        manejarEntregaConfirmada(evento);
        break;
      case "ENTREGA_FALLIDA":
        manejarEntregaFallida(evento, contactosAdministradores);
        break;
      case "REINGRESO_DEPOSITO":
        manejarReingresoDeposito(evento);
        break;
      default:
        System.out.println("Evento de logística desconocido: " + evento.getTipoEvento());
    }
  }

  private void manejarInicioRuta(EventoLogisticaDTO evento) {
    if (evento.getPayloadJson() == null || evento.getPayloadJson().isEmpty()) {
      System.err.println("Evento INICIO_RUTA " + evento.getId() + " sin payload, se ignora.");
      return;
    }

    PayloadInicioRutaDTO payload;
    try {
      payload = objectMapper.readValue(evento.getPayloadJson(), PayloadInicioRutaDTO.class);
    } catch (Exception e) {
      System.err.println("Error parseando payload de la ruta " + evento.getId() + ": " + e.getMessage());
      return;
    }

    if (payload.getItems() == null) {
      return;
    }

    EstrategiaNotificacion estrategiaViaje = fabricaEstrategias.obtenerEstrategia(TipoEventoNotificacion.DONACION_EN_VIAJE);

    // Try/catch DENTRO del loop: antes envolvía todo el for, así que un solo item con un UUID
    // malformado o una donación sin entidad/donante asignado (NPE al armar la notificación)
    // abortaba el procesamiento del resto de los items de ESTE evento INICIO_RUTA.
    for (String idTexto : payload.getItems()) {
      try {
        UUID idDonacion = UUID.fromString(idTexto);
        repositorioDonaciones.obtenerPorId(idDonacion).ifPresent(donacion -> {
          donacion.actualizarEstado(Estado.EN_TRASLADO, "Ruta iniciada por Logística");
          repositorioDonaciones.guardar(donacion);
          estrategiaViaje.ejecutar(new NotificacionViajeDatos(
              payload.getUrlRuta(),
              donacion.getDonante().getPersona().getMediosDeContacto(),
              donacion.getEntidad().getPersonaJuridica().getMediosDeContacto()
          ));
        });
      } catch (Exception e) {
        System.err.println("Error procesando item '" + idTexto + "' de la ruta " + evento.getId() + ": " + e.getMessage());
      }
    }
  }

  private void manejarEntregaConfirmada(EventoLogisticaDTO evento) {
    UUID idDonacion = UUID.fromString(evento.getReferenciaId());
    repositorioDonaciones.obtenerPorId(idDonacion).ifPresent(donacion -> {
      donacion.actualizarEstado(Estado.ENTREGADO, "Entrega confirmada por la entidad");
      repositorioDonaciones.guardar(donacion);

      // Recién ahora la donación está ENTREGADA: es el momento en el que incentivos cuenta el
      // impacto (sus reglas de misión exigen estado "ENTREGADA", no "ASIGNADO"). El id va como
      // clave de idempotencia: un reintento del evento no duplica el impacto.
      incentivosClient.notificarImpactoDonacion(
          donacion.getDonante().getId(), IncentivosDonacionDTO.desde(donacion));

      PayloadEntregaDTO payload = parsearPayloadEntrega(evento);

      fabricaEstrategias.ejecutar(TipoEventoNotificacion.COMPROBANTE_ENTREGA,
              new NotificacionEntregaDatos(
                  payload,
                  donacion.getDonante().getPersona().getMediosDeContacto(),
                  donacion.getEntidad().getPersonaJuridica().getMediosDeContacto()
      ));
    });
  }

  private void manejarEntregaFallida(EventoLogisticaDTO evento, List<MedioDeContacto> contactosAdministradores) {
    UUID idDonacion = UUID.fromString(evento.getReferenciaId());
    repositorioDonaciones.obtenerPorId(idDonacion).ifPresent(donacion -> {
      String justificacion = evento.getJustificacion() != null ? evento.getJustificacion() : "Falla reportada en destino";
      donacion.actualizarEstado(Estado.EN_DEPOSITO, justificacion);
      repositorioDonaciones.guardar(donacion);

      PayloadEntregaDTO payload = parsearPayloadEntrega(evento);
      fabricaEstrategias.ejecutar(TipoEventoNotificacion.ENTREGA_NO_RECIBIDA,
              new NotificacionEntregaFallidaDatos(
                  payload,
                  donacion.getDonante().getPersona().getMediosDeContacto(),
                  donacion.getEntidad().getPersonaJuridica().getMediosDeContacto(),
                  contactosAdministradores
      ));
    });
  }

  private void manejarReingresoDeposito(EventoLogisticaDTO evento) {
    UUID idDonacion = UUID.fromString(evento.getReferenciaId());
    Optional<Donacion> donacionOpt = repositorioDonaciones.obtenerPorId(idDonacion);

    if (donacionOpt.isPresent()) {
      Donacion donacion = donacionOpt.get();
      donacion.actualizarEstado(Estado.PENDIENTE_ASIGNACION, "Reingresado a depósito tras revisión de entrega fallida");
      repositorioDonaciones.guardar(donacion);
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
}