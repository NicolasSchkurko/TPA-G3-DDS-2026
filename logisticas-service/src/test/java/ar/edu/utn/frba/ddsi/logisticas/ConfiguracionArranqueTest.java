package ar.edu.utn.frba.ddsi.logisticas;

import ar.edu.utn.frba.ddsi.logisticas.Scheduler.PlanificadorDeRutasScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dos cosas que no se ven hasta que ya rompio algo, y por eso conviene que fallen aqui.
 *
 * <p><b>La credencial de la base no puede estar como default.</b> Con
 * {@code ${DB_USERNAME:valentin}}, cualquier arranque sin las variables —un {@code java -jar} a
 * secas, un deploy que se olvido de definirlas— conectaba igual, en claro y sin avisar. Ahora
 * {@code application.properties} no tiene default: si falta la variable, el servicio no levanta.
 *
 * <p><b>El cron dice 2 de la mañana porque el horario va escrito.</b> Spring usa la zona de la
 * JVM cuando el cron no declara {@code zone}. La imagen final no define {@code TZ}, el compose no
 * le pasa ninguna, y el {@code DB_URL} fuerza {@code serverTimezone=UTC}: en Docker la
 * planificación corría a las 02:00 UTC, que son las 23:00 de Argentina, y en un host con otra
 * zona seria una hora distinta. Declararla la deja de depender de dónde corra.
 */
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

    // --- Punto 33: la credencial de la base ---

    @Test
    @DisplayName("Las credenciales de la base no tienen default: si faltan, no se arranca")
    void lasCredencialesNoTienenDefault() throws IOException {
        Properties propiedades = new Properties();
        propiedades.load(new java.io.StringReader(applicationProperties()));

        // `${DB_USERNAME}` a secas: el placeholder sin nada despues de los dos puntos. Con un
        // default (`:algo`) el servicio conectaba igual sin que nadie definiera la variable.
        assertThat(propiedades.getProperty("spring.datasource.username"))
                .as("con default, un deploy sin variables conecta con la credencial del codigo")
                .isEqualTo("${DB_USERNAME}");
        assertThat(propiedades.getProperty("spring.datasource.password"))
                .isEqualTo("${DB_PASSWORD}");
    }

    /**
     * Ninguna property de credenciales puede traer un valor por defecto.
     *
     * <p>Estructural a proposito, y no buscando la clave que estaba escrita: un test que
     * comprueba que "no aparece tal contraseña" tiene que escribirla para nombrarla, y entonces
     * la deja en el repo, que es justo lo que se esta evitando. Ademas asi detecta el default que
     * sea, y no solo el de un despliegue.
     */
    @Test
    @DisplayName("Ninguna property de credenciales trae un valor por defecto")
    void ningunaCredencialTieneDefault() throws IOException {
        Properties propiedades = new Properties();
        propiedades.load(new java.io.StringReader(applicationProperties()));

        for (String clave : List.of("spring.datasource.username", "spring.datasource.password")) {
            String valor = propiedades.getProperty(clave);
            assertThat(valor)
                    .as("%s no debe traer nada despues de los dos puntos", clave)
                    .doesNotContain(":");
        }
    }

    // --- Punto 39: la zona del cron ---

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

    // --- Punto 25: la DLQ tiene que ser alcanzable ---

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