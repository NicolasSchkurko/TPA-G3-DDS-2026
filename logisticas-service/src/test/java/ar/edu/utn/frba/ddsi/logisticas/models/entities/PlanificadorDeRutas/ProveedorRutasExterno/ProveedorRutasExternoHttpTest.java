package ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProveedorRutasExternoHttpTest {

  @Test
  @DisplayName("un lote con LocalDateTime se serializa y llega al proveedor externo")
  void enviaElLoteConFechasAlProveedorExterno() throws Exception {
    CountDownLatch recibido = new CountDownLatch(1);
    AtomicReference<String> cuerpo = new AtomicReference<>();

    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/rutas", exchange -> {
      cuerpo.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      exchange.sendResponseHeaders(200, -1);
      exchange.close();
      recibido.countDown();
    });
    server.start();

    try {
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/rutas";
      ProveedorRutasExternoHttp proveedor = new ProveedorRutasExternoHttp(url);

      ItemEntrega item = new ItemEntrega(UUID.randomUUID(), 3, UnidadDeMedida.UNIDADES, null);
      item.setFechaCambioEstado(LocalDateTime.of(2026, 1, 2, 3, 4, 5));
      item.setEventos(List.of(new EventoLogistica(
              "ENTREGADO", "ref-1", LocalDateTime.of(2026, 1, 2, 3, 4, 5), null)));

      proveedor.solicitarPlanificacion(List.of(item), List.of());

      assertTrue(recibido.await(5, TimeUnit.SECONDS),
              "El lote con LocalDateTime debe serializarse y enviarse al proveedor externo");
      assertTrue(cuerpo.get().contains("\"fechaCambioEstado\":\"2026-01-02T03:04:05\""),
              "Las fechas deben viajar en ISO-8601: " + cuerpo.get());
      assertFalse(cuerpo.get().contains("\"fechaCambioEstado\":["),
              "Las fechas no deben viajar como array numérico: " + cuerpo.get());
    } finally {
      server.stop(0);
    }
  }

  @Test
  @DisplayName("una falla de red se loguea en vez de descartarse en silencio")
  void unaFallaDeRedSeLoguea() throws Exception {
    // Puerto 1: no hay nada escuchando, la conexión se rechaza en el acto.
    String url = "http://127.0.0.1:1/rutas";

    Logger logger = (Logger) LoggerFactory.getLogger(ProveedorRutasExternoHttp.class);
    CountDownLatch logueado = new CountDownLatch(1);
    AtomicReference<ILoggingEvent> error = new AtomicReference<>();
    AppenderBase<ILoggingEvent> appender = new AppenderBase<>() {
      @Override
      protected void append(ILoggingEvent evento) {
        if (evento.getLevel() == Level.ERROR
                && evento.getFormattedMessage().contains(url)) {
          error.set(evento);
          logueado.countDown();
        }
      }
    };
    appender.start();
    logger.addAppender(appender);

    try {
      new ProveedorRutasExternoHttp(url).solicitarPlanificacion(List.of(), List.of());

      assertTrue(logueado.await(15, TimeUnit.SECONDS),
              "la falla de red debe quedar registrada en el log");
      assertTrue(error.get().getFormattedMessage().contains("proveedor externo de ruteo"));
      assertTrue(error.get().getThrowableProxy() != null,
              "el log debe conservar el stack trace de la excepción");
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }
}
