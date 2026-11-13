package ar.edu.utn.frba.ddsi.incentivos.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Pools acotados para tareas de red fuera del hilo que commitea o del scheduler.
 * Si una cola se llena, {@code CallerRunsPolicy} ejecuta la tarea en el hilo llamador
 * como backpressure deliberado en vez de descartarla.
 */
@Configuration
public class AsyncExecutorConfig {

    /** Pool acotado de 4 hilos con cola de 1000 tareas. */
    @Bean(name = "notificacionExecutor")
    public ThreadPoolTaskExecutor notificacionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("notificacion-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /** Pool acotado de 4 hilos para publicar en n8n sin bloquear el scheduler. */
    @Bean(name = "n8nExecutor")
    public ThreadPoolTaskExecutor n8nExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("n8n-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
