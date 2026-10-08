package ar.edu.utn.frba.ddsi.notificaciones;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Configuración que no rompe un test hasta que está en producción: el log de SQL apagado por
 * defecto y la cola sin reintentos infinitos.
 */
class ConfiguracionArranqueTest {

    private static Properties propiedadesDe(String recurso) throws IOException {
        try (InputStream entrada = ConfiguracionArranqueTest.class.getResourceAsStream(recurso)) {
            assertThat(entrada).as(recurso + " tiene que estar en el classpath").isNotNull();

            Properties propiedades = new Properties();
            propiedades.load(entrada);
            return propiedades;
        }
    }

    @Test
    @DisplayName("El SQL no se loguea por defecto")
    void showSqlNoEstaEnLaConfiguracionPorDefecto() throws IOException {
        String valor = propiedadesDe("/application.properties").getProperty("spring.jpa.show-sql");

        assertThat(valor == null || !valor.trim().equalsIgnoreCase("true"))
                .as("en una instancia desplegada el SQL en el log es ruido y puede filtrar datos")
                .isTrue();
    }

    @Test
    @DisplayName("El perfil dev sí prende el log de SQL")
    void elPerfilDevPrendeShowSql() throws IOException {
        assertThat(propiedadesDe("/application-dev.properties").getProperty("spring.jpa.show-sql"))
                .as("si no, no queda forma de prenderlo y depurar contra la base local")
                .isEqualTo("true");
    }

    @Test
    @DisplayName("Un mensaje rechazado va a la DLQ, no vuelve a la cola")
    void loRechazadoNoSeReencola() throws IOException {
        assertThat(propiedadesDe("/application.properties")
                .getProperty("spring.rabbitmq.listener.simple.default-requeue-rejected"))
                .as("con el default (true) el mensaje vuelve a la cola y se reintenta para siempre")
                .isEqualTo("false");
    }

    @Test
    @DisplayName("El reintento es acotado y no infinito")
    void elReintentoEsAcotado() throws IOException {
        Properties propiedades = propiedadesDe("/application.properties");

        assertThat(propiedades.getProperty("spring.rabbitmq.listener.simple.retry.enabled"))
                .isEqualTo("true");
        assertThat(propiedades.getProperty("spring.rabbitmq.listener.simple.retry.max-attempts"))
                .as("sin tope, el reintento es el mismo loop con otro nombre")
                .isEqualTo("3");
    }
}
