package ar.edu.utn.frba.ddsi.logisticas;

import ar.edu.utn.frba.ddsi.logisticas.scheduler.PlanificadorDeRutasScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("La configuracion no deja credenciales ni horarios surprises")
class ConfiguracionArranqueTest {

    private static String applicationProperties() throws IOException {
        try (InputStream entrada =
                     ConfiguracionArranqueTest.class.getResourceAsStream("/application.properties")) {
            assertThat(entrada).as("application.properties tiene que estar en el classpath")
                    .isNotNull();
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Las credenciales de la base no tienen default: si faltan, no se arranca")
    void lasCredencialesNoTienenDefault() throws IOException {
        Properties propiedades = new Properties();
        propiedades.load(new java.io.StringReader(applicationProperties()));

        assertThat(propiedades.getProperty("spring.data.mongodb.uri"))
                .as("con default, un deploy sin variables conecta con la credencial del codigo")
                .isEqualTo("${MONGODB_URI}");
    }

    @Test
    @DisplayName("Ninguna property de credenciales trae un valor por defecto")
    void ningunaCredencialTieneDefault() throws IOException {
        Properties propiedades = new Properties();
        propiedades.load(new java.io.StringReader(applicationProperties()));

        for (String clave : List.of("spring.data.mongodb.uri")) {
            String valor = propiedades.getProperty(clave);
            assertThat(valor)
                    .as("%s no debe traer nada despues de los dos puntos", clave)
                    .doesNotContain(":");
        }
    }

    @Test
    @DisplayName("El cron de planificacion declara la zona, no la hereda de la JVM")
    void elCronDeclaraLaZona() throws NoSuchMethodException {
        Scheduled programado = PlanificadorDeRutasScheduler.class
                .getDeclaredMethod("iniciarPlanificacionAutomatica")
                .getAnnotation(Scheduled.class);

        // Sin zone, Spring usa la zona de la JVM: en Docker eso es UTC, y el cron de las 2
        // corria a las 23:00 de Argentina.
        assertThat(programado.zone())
                .as("sin zone el horario depende de la imagen y del host")
                .isEqualTo("America/Argentina/Buenos_Aires");

        // Y la expresion del cron no se toca: sigue siendo a las 2.
        assertThat(programado.cron()).isEqualTo("0 0 2 * * ?");
    }

    @Test
    @DisplayName("Punto 72: el contenedor acota el heap y la memoria")
    void elContenedorAcotaLaMemoria() throws IOException {
        String dockerfile = Files.readString(Path.of("Dockerfile"));
        assertThat(dockerfile)
                .contains("-XX:MaxRAMPercentage=70.0")
                .contains("-XX:+ExitOnOutOfMemoryError");

        String compose = Files.readString(Path.of("..", "docker-compose.yml"));
        assertThat(compose)
                .containsPattern("(?s)logisticas-service:.*?mem_limit:\\s*512m");
    }

    @Test
    @DisplayName("Un mensaje que el listener descarta va a la DLQ, no vuelve a la cola")
    void loDescartadoVaALaDlq() throws IOException {
        Properties propiedades = new Properties();
        propiedades.load(new java.io.StringReader(applicationProperties()));

        // Con el default de Spring AMQP (true), lo que sale del listener se reencola: el mismo
        // mensaje vuelve a fallar para siempre y frena la cola compartida, sin tocar la DLQ.
        assertThat(propiedades.getProperty("spring.rabbitmq.listener.simple.default-requeue-rejected"))
                .isEqualTo("false");
    }
}