package ar.edu.utn.frba.ddsi.logisticas.config;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno.ProveedorRutasExternoSimulado;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.ProveedorRutasExterno.ProveedorRutasExterno;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogisticaConfig {

  @Bean
  public ProveedorRutasExterno proveedorRutasExterno() {
    return new ProveedorRutasExternoSimulado();
  }
}