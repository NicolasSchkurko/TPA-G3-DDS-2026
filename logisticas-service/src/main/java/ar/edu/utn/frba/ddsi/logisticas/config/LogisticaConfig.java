package ar.edu.utn.frba.ddsi.logisticas.config;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno.ProveedorRutasExternoSimulado;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno.ProveedorRutasExterno;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class LogisticaConfig {

  @Bean
  public Executor ejecutorRuteo() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(1);
    executor.setQueueCapacity(Integer.MAX_VALUE);
    executor.setThreadNamePrefix("ruteo-");
    executor.setDaemon(true);
    return executor;
  }

  @Bean
  public ProveedorRutasExterno proveedorRutasExterno(
      @Qualifier("ejecutorRuteo") Executor ejecutorRuteo,
      @Value("${logistica.callback-url}") String callbackUrl) {
    return new ProveedorRutasExternoSimulado(ejecutorRuteo, callbackUrl);
  }
}