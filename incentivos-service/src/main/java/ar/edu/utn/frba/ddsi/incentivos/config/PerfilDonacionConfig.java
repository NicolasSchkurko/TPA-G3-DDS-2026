package ar.edu.utn.frba.ddsi.incentivos.config;

//estoy desde el celu, intellij escribe los import

//config para conectar como cliente a servicio donaciones y notificaciones
@Configuration
public class PerfilDonacionConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
            .baseUrl("http://localhost:8081") // URL base del Servicio B (Donaciones)
            .build();
    }
}
