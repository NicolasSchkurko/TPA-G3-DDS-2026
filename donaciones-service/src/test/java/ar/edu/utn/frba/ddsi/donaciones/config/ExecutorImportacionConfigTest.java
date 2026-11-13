package ar.edu.utn.frba.ddsi.donaciones.config;

import ar.edu.utn.frba.ddsi.donaciones.DonacionesServiceApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutorImportacionConfigTest {

    @Test
    @DisplayName("Punto 71: Los hilos del executor de importación no deben ser daemon para no abortar en shutdown")
    void hilosDeImportacionNoDebenSerDaemon() throws Exception {
        DonacionesServiceApplication app = new DonacionesServiceApplication();
        ThreadPoolTaskExecutor taskExecutor = app.taskExecutorImportacion();
        ExecutorService executor = app.executorImportacion(taskExecutor);

        AtomicBoolean eraDaemon = new AtomicBoolean(true);
        CountDownLatch latch = new CountDownLatch(1);

        executor.submit(() -> {
            eraDaemon.set(Thread.currentThread().isDaemon());
            latch.countDown();
        });

        boolean completo = latch.await(5, TimeUnit.SECONDS);
        taskExecutor.destroy();

        assertTrue(completo, "La tarea debe ejecutarse");
        assertFalse(eraDaemon.get(), "Los hilos de importación deben ser no-daemon (user threads) para evitar abortos prematuros");
    }

    @Test
    @DisplayName("Punto 71: El executor debe tener configurado apagado ordenado con espera de tareas activas")
    void executorConfiguradoConApagadoOrdenado() throws Exception {
        DonacionesServiceApplication app = new DonacionesServiceApplication();
        ThreadPoolTaskExecutor taskExecutor = app.taskExecutorImportacion();
        ExecutorService executor = app.executorImportacion(taskExecutor);

        assertNotNull(executor, "El bean ExecutorService debe estar instanciado");
        assertFalse(taskExecutor.getThreadPoolExecutor().isTerminating() || taskExecutor.getThreadPoolExecutor().isShutdown());

        // Verificamos que taskExecutor espera tareas activas durante el cierre del contexto
        AtomicBoolean tareaFinalizada = new AtomicBoolean(false);
        CountDownLatch tareaIniciada = new CountDownLatch(1);

        executor.submit(() -> {
            tareaIniciada.countDown();
            try {
                Thread.sleep(200);
                tareaFinalizada.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(tareaIniciada.await(2, TimeUnit.SECONDS), "La tarea debe haber comenzado");

        // Al invocar destroy() (equivalente al shutdown del contexto Spring), debe bloquear esperando que la tarea termine
        taskExecutor.destroy();

        assertTrue(tareaFinalizada.get(), "El pool debe esperar a que la tarea en vuelo termine antes de finalizar destroy()");
        assertTrue(taskExecutor.getThreadPoolExecutor().isTerminated(), "El pool subyacente debe quedar terminado");
    }
}
