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
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiaNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.exceptions.EventoYaProcesadoException;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeEventosProcesados;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class GestorEventosLogistica {
  private static final Logger log = LoggerFactory.getLogger(GestorEventosLogistica.class);

  private final RepositorioDeDonaciones repositorioDonaciones;
  private final FabricaEstrategiasNotificacion fabricaEstrategias;
  private final IncentivosClient incentivosClient;
  private final RepositorioDeEventosProcesados repoEventosProcesados;
  private final ObjectMapper objectMapper;
  private final GestorIncentivos gestorIncentivos;

  public GestorEventosLogistica(RepositorioDeDonaciones repositorioDonaciones,
                                FabricaEstrategiasNotificacion fabricaEstrategias,
                                IncentivosClient incentivosClient,
                                RepositorioDeEventosProcesados repoEventosProcesados,
                                ObjectMapper objectMapper) {
    this(repositorioDonaciones, fabricaEstrategias, incentivosClient,
        repoEventosProcesados, objectMapper, null);
  }

  @Autowired
  public GestorEventosLogistica(RepositorioDeDonaciones repositorioDonaciones,
                                FabricaEstrategiasNotificacion fabricaEstrategias,
                                IncentivosClient incentivosClient,
                                RepositorioDeEventosProcesados repoEventosProcesados,
                                ObjectMapper objectMapper,
                                GestorIncentivos gestorIncentivos) {
    this.repositorioDonaciones = repositorioDonaciones;
    this.fabricaEstrategias = fabricaEstrategias;
    this.incentivosClient = incentivosClient;
    this.repoEventosProcesados = repoEventosProcesados;
    this.objectMapper = objectMapper;
    this.gestorIncentivos = gestorIncentivos;
  }

  @Transactional(rollbackFor = Exception.class)
  public void procesarEvento(EventoLogisticaDTO evento, List<MedioDeContacto> contactosAdministradores) {
    reclamarEvento(evento.getId());

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

  /**
   * Inserta la fila de deduplicación al comienzo de esta transacción.
   *
   * <p>Si otra réplica ya reclamó el evento, la violación de clave primaria se traduce a
   * {@link EventoYaProcesadoException}: el listener la confirma sin reprocesar. Cualquier
   * otra falla de base de datos durante el procesamiento se propaga como está para que el
   * broker reintente.
   */
  private void reclamarEvento(Long idEvento) {
    try {
      repoEventosProcesados.reclamar(idEvento);
    } catch (DataIntegrityViolationException carrera) {
      throw new EventoYaProcesadoException(idEvento, carrera);
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
      // Punto 61: La llamada se ejecuta post-commit (afterCommit) o aislada en try/catch para
      // que una falla externa de red/500 en incentivos-service no aborte la transacción de entrega.
      UUID donanteId = donacion.getDonante() != null ? donacion.getDonante().getId() : null;
      if (donanteId != null) {
        IncentivosDonacionDTO dto = IncentivosDonacionDTO.desde(donacion);
        Runnable notificarIncentivos;
        if (gestorIncentivos != null) {
          UUID outboxId = gestorIncentivos.encolarImpacto(donanteId, dto);
          notificarIncentivos = () -> gestorIncentivos.intentarAhora(outboxId);
        } else {
          notificarIncentivos = () -> {
            try {
              incentivosClient.notificarImpactoDonacion(donanteId, dto);
            } catch (Exception e) {
              log.warn("No se pudo notificar el impacto a incentivos-service para la donación {} (donante {}): {}",
                  idDonacion, donanteId, e.getMessage());
            }
          };
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
          TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              notificarIncentivos.run();
            }
          });
        } else {
          notificarIncentivos.run();
        }
      }

      PayloadEntregaDTO payload = parsearPayloadEntrega(evento);

      try {
        fabricaEstrategias.ejecutar(TipoEventoNotificacion.COMPROBANTE_ENTREGA,
                new NotificacionEntregaDatos(
                    payload,
                    donacion.getDonante() != null && donacion.getDonante().getPersona() != null
                        ? donacion.getDonante().getPersona().getMediosDeContacto()
                        : new MediosDeContacto(),
                    donacion.getEntidad() != null && donacion.getEntidad().getPersonaJuridica() != null
                        ? donacion.getEntidad().getPersonaJuridica().getMediosDeContacto()
                        : new MediosDeContacto()
        ));
      } catch (Exception e) {
        log.warn("No se pudo enviar la notificación de entrega para la donación {}: {}", idDonacion, e.getMessage());
      }
    });
  }

  private void manejarEntregaFallida(EventoLogisticaDTO evento, List<MedioDeContacto> contactosAdministradores) {
    UUID idDonacion = UUID.fromString(evento.getReferenciaId());
    repositorioDonaciones.obtenerPorId(idDonacion).ifPresent(donacion -> {
      String justificacion = evento.getJustificacion() != null ? evento.getJustificacion() : "Falla reportada en destino";
      donacion.actualizarEstado(Estado.ENTREGA_FALLIDA, justificacion);
      repositorioDonaciones.guardar(donacion);

      PayloadEntregaDTO payload = parsearPayloadEntrega(evento);
      try {
        fabricaEstrategias.ejecutar(TipoEventoNotificacion.ENTREGA_NO_RECIBIDA,
                new NotificacionEntregaFallidaDatos(
                    payload,
                    donacion.getDonante() != null && donacion.getDonante().getPersona() != null
                        ? donacion.getDonante().getPersona().getMediosDeContacto()
                        : new MediosDeContacto(),
                    donacion.getEntidad() != null && donacion.getEntidad().getPersonaJuridica() != null
                        ? donacion.getEntidad().getPersonaJuridica().getMediosDeContacto()
                        : new MediosDeContacto(),
                    contactosAdministradores
        ));
      } catch (Exception e) {
        log.warn("No se pudo enviar la notificación de entrega fallida para la donación {}: {}", idDonacion, e.getMessage());
      }
    });
  }

  private void manejarReingresoDeposito(EventoLogisticaDTO evento) {
    UUID idDonacion = UUID.fromString(evento.getReferenciaId());
    Optional<Donacion> donacionOpt = repositorioDonaciones.obtenerPorId(idDonacion);

    if (donacionOpt.isPresent()) {
      Donacion donacion = donacionOpt.get();
      donacion.actualizarEstado(Estado.EN_DEPOSITO, "Reingresado a depósito tras revisión de entrega fallida");
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
