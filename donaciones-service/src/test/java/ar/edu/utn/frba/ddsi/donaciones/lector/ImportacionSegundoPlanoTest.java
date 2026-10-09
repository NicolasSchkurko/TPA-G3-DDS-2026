package ar.edu.utn.frba.ddsi.donaciones.lector;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IDDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.ReporteImportacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.MapeoCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioCiudades;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioDonantes;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioPersonas;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La importación corre en un pool propio con cola acotada y el alta de perfiles va en lote:
 * <ul>
 *   <li>con la cola llena se rechaza (el controller lo traduce a 429) sin reporte fantasma;</li>
 *   <li>el reporte se cierra siempre, incluso ante un {@code Error} que mataría al worker;</li>
 *   <li>los perfiles van a incentivos en un solo POST /api/perfiles/lote, no uno por fila.</li>
 * </ul>
 */
public class ImportacionSegundoPlanoTest {

  private static final byte[] CSV_MINIMO =
      "TipoPersona,Nombre,Documento\nHUMANA,Ana Navarro,1\n".getBytes(StandardCharsets.UTF_8);

  private RepositorioPersonas repoPersonas;
  private IncentivosClient incentivos;

  private List<MapeoCSV> mapeos() {
    return List.of(
        new MapeoCSV("TIPO_PERSONA", List.of("TipoPersona")),
        new MapeoCSV("NOMBRE_RAZON_SOCIAL", List.of("Nombre")),
        new MapeoCSV("DOCUMENTO", List.of("Documento")));
  }

  private DonanteService servicioCon(ExecutorService executor) {
    repoPersonas = mock(RepositorioPersonas.class);
    incentivos = mock(IncentivosClient.class);
    return new DonanteService(
        repoPersonas,
        mock(RepositorioCiudades.class),
        mock(FabricaEstrategiasNotificacion.class),
        incentivos,
        mock(RepositorioDonantes.class),
        executor);
  }

  private MockMultipartFile archivo() {
    return new MockMultipartFile("file", "donantes.csv", "text/csv", CSV_MINIMO);
  }

  private static ExecutorService poolDeUnHilo() {
    return Executors.newSingleThreadExecutor(tarea -> {
      Thread hilo = new Thread(tarea, "test-importacion");
      hilo.setDaemon(true);
      return hilo;
    });
  }

  @Test
  @DisplayName("Con el pool y la cola llenos, la importación se rechaza en vez de encolarse")
  void colaLLena_rechazaLaImportacion() throws Exception {
    ExecutorService saturado = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(1),
        tarea -> {
          Thread hilo = new Thread(tarea);
          hilo.setDaemon(true);
          return hilo;
        });
    CountDownLatch liberar = new CountDownLatch(1);
    saturado.execute(() -> esperar(liberar));
    saturado.execute(() -> esperar(liberar)); // ya llena la cola de 1

    try {
      DonanteService servicio = servicioCon(saturado);
      assertThrows(RejectedExecutionException.class,
          () -> servicio.importarDonantes(archivo(), mapeos()),
          "con worker y cola ocupados la importación no puede quedar encolada en silencio");
    } finally {
      liberar.countDown();
      saturado.shutdownNow();
    }
  }

  @Test
  @DisplayName("Un Error en el worker igual deja el reporte COMPLETADO, nunca EN_PROGRESO eterno")
  void unErrorCierraElReporteIgual() throws Exception {
    ExecutorService unHilo = poolDeUnHilo();

    try {
      DonanteService servicio = servicioCon(unHilo);
      doThrow(new StackOverflowError("falla simulada del worker"))
          .when(repoPersonas).registrarPersona(any());

      UUID importId = servicio.importarDonantes(archivo(), mapeos());

      ReporteImportacionDTO reporte = esperarCompletado(servicio, importId, Duration.ofSeconds(10));
      assertEquals("COMPLETADO", reporte.getEstado(),
          "el reporte no puede quedar EN_PROGRESO para siempre si el worker muere con un Error");
    } finally {
      unHilo.shutdownNow();
    }
  }

  @Test
  @DisplayName("Los perfiles van a incentivos en un solo lote, no uno por fila")
  void losPerfilesVanEnLote() throws Exception {
    ExecutorService unHilo = poolDeUnHilo();

    try {
      DonanteService servicio = servicioCon(unHilo);
      UUID importId = servicio.importarDonantes(archivo(), mapeos());
      esperarCompletado(servicio, importId, Duration.ofSeconds(10));

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<IDDTO>> captor = ArgumentCaptor.forClass(List.class);
      verify(incentivos).peticionCrearPerfilesEnLote(captor.capture());

      List<IDDTO> lote = captor.getValue();
      assertEquals(1, lote.size(), "una fila, un perfil en el lote");
      assertEquals("Ana Navarro", lote.get(0).getNombreUsuario());
      assertEquals("DONANTE", lote.get(0).getRole());
    } finally {
      unHilo.shutdownNow();
    }
  }

  private ReporteImportacionDTO esperarCompletado(DonanteService servicio, UUID importId,
                                                  Duration timeout) throws InterruptedException {
    long limite = System.nanoTime() + timeout.toNanos();
    ReporteImportacionDTO ultimo = null;
    while (System.nanoTime() < limite) {
      ultimo = servicio.obtenerReporteImportacion(importId);
      if ("COMPLETADO".equals(ultimo.getEstado())) {
        return ultimo;
      }
      Thread.sleep(20);
    }
    return fail("El reporte quedó en " + (ultimo == null ? "?" : ultimo.getEstado()));
  }

  private static void esperar(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
