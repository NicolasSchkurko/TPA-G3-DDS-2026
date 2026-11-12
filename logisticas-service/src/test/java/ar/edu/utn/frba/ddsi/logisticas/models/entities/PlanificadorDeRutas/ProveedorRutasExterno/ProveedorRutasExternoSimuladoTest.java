package ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno;

import ar.edu.utn.frba.ddsi.logisticas.config.LogisticaConfig;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ProveedorRutasExternoSimuladoTest {

  private static final String URL_CALLBACK = "http://localhost:1/api/PlanificacionRutas/callback";

  @Test
  @DisplayName("la planificación simulada se delega al executor provisto, no al commonPool")
  void usaElExecutorProvisto() {
    AtomicReference<Runnable> tarea = new AtomicReference<>();
    Executor executor = tarea::set;

    new ProveedorRutasExternoSimulado(executor, URL_CALLBACK)
            .solicitarPlanificacion(List.of(), List.of());

    assertThat(tarea.get())
            .as("si se ejecutara en ForkJoinPool.commonPool este executor no recibiría nada")
            .isNotNull();
  }

  @Test
  @DisplayName("El pool de ruteo encola sin tope en vez de rechazar planificaciones")
  void elPoolNoRechazaPlanificaciones() {
    ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new LogisticaConfig().ejecutorRuteo();
    executor.initialize();

    assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity())
            .isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  @DisplayName("la property logistica.callback-url llega al proveedor")
  void laPropertyDeCallbackLlegaAlProveedor() throws IOException {
    CountDownLatch recibido = new CountDownLatch(1);
    HttpServer server = servidor(recibido);
    try {
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/callback";

      new ApplicationContextRunner()
              .withUserConfiguration(LogisticaConfig.class)
              .withPropertyValues("logistica.callback-url=" + url)
              .run(context -> {
                context.getBean(ProveedorRutasExterno.class)
                        .solicitarPlanificacion(List.of(), List.of());

                assertThat(recibido.await(10, TimeUnit.SECONDS))
                        .as("el bean debe usar la URL de la property")
                        .isTrue();
              });
    } finally {
      server.stop(0);
    }
  }

  @Test
  @DisplayName("un callback que no responde 2xx se loguea como error con la URL")
  void unCallbackFallidoSeLoguea() throws IOException {
    CountDownLatch recibido = new CountDownLatch(1);
    HttpServer server = servidor(recibido, 500);
    Logger logger = (Logger) LoggerFactory.getLogger(ProveedorRutasExternoSimulado.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    try {
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/callback";

      new ProveedorRutasExternoSimulado(Runnable::run, url)
              .solicitarPlanificacion(List.of(), List.of());

      assertThat(appender.list).anySatisfy(evento -> {
        assertThat(evento.getLevel()).isEqualTo(Level.ERROR);
        assertThat(evento.getFormattedMessage()).contains("500").contains(url);
      });
    } finally {
      logger.detachAppender(appender);
      server.stop(0);
    }
  }

  private static HttpServer servidor(CountDownLatch recibido) throws IOException {
    return servidor(recibido, 200);
  }

  private static HttpServer servidor(CountDownLatch recibido, int status) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/callback", exchange -> {
      exchange.getRequestBody().readAllBytes();
      if (status >= 400) {
        byte[] cuerpo = "error".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, cuerpo.length);
        exchange.getResponseBody().write(cuerpo);
      } else {
        exchange.sendResponseHeaders(status, -1);
      }
      exchange.close();
      recibido.countDown();
    });
    server.start();
    return server;
  }
}
