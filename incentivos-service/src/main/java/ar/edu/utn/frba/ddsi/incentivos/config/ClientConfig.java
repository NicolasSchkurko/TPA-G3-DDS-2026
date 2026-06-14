package ar.edu.utn.frba.ddsi.incentivos.config;

//estoy desde el celu, intellij escribe los import

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

//config para conectar como cliente a servicio donaciones y notificaciones
@Configuration
public class ClientConfig {

    @Bean
    public RestClient generalClient() {
        return RestClient.builder().build(); // Sin baseUrl fija para consumir +1 servicio
    }
}
