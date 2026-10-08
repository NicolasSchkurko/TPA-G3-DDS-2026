package ar.edu.utn.frba.ddsi.donaciones;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las credenciales de la base no viven en el codigo.
 *
 * <p>Vienen del {@code .env} del modulo, que Docker Compose lee con {@code env_file} y que el
 * {@code .gitignore} mantiene afuera del repo. Con un default en el placeholder — {@code
 * ${DB_USERNAME:algo}} — estas lineas conectan igual cuando la variable no esta: un despliegue se
 * levanta contra la credencial que quedo escrita en el fuente en vez de fallar y delatar que le
 * falta la suya.
 *
 * <p>La comprobacion es estructural a proposito. Un test que verifica "no aparece tal contrasena"
 * tiene que escribirla para nombrarla, y entonces la deja en el repo, que es justo lo que se
 * quiere evitar. Asi, en cambio, detecta cualquier default, sin conocer ninguno.
 */
@DisplayName("La configuracion no deja credenciales por defecto")
class ConfiguracionArranqueTest {

    private static final List<String> CREDENCIALES =
            List.of("spring.datasource.username", "spring.datasource.password");

    private static String applicationProperties() throws IOException {
        try (InputStream entrada =
                     ConfiguracionArranqueTest.class.getResourceAsStream("/application.properties")) {
            assertThat(entrada).as("application.properties tiene que estar en el classpath")
                    .isNotNull();
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Usuario y clave no traen default: si faltan, el servicio no levanta")
    void lasCredencialesNoTienenDefault() throws IOException {
        Properties propiedades = new Properties();
        propiedades.load(new java.io.StringReader(applicationProperties()));

        for (String clave : CREDENCIALES) {
            assertThat(propiedades.getProperty(clave))
                    .as("%s deberia ser el placeholder pelado, sin nada despues de los dos puntos", clave)
                    .doesNotContain(":");
        }
    }

    /**
     * La clave tiene que estar en la variable de entorno y no en el archivo.
     *
     * <p>Esto es mas fuerte que "no hay default": exige que la propiedad diga exactamente
     * {@code ${DB_...}}, o sea que el valor solo pueda venir de afuera.
     */
    @Test
    @DisplayName("Las credenciales apuntan a la variable de entorno, sin default ni valor fijo")
    void lasCredencialesVienenDeLaVariableDeEntorno() throws IOException {
        Properties propiedades = new Properties();
        propiedades.load(new java.io.StringReader(applicationProperties()));

        assertThat(propiedades.getProperty("spring.datasource.username")).isEqualTo("${DB_USERNAME}");
        assertThat(propiedades.getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
    }

    /** La URL si puede tener default: no es una credencial, solo dice que base usar. */
    @Test
    @DisplayName("La URL de la base conserva su default, que no es secreto")
    void laUrlConservaSuDefault() throws IOException {
        Properties propiedades = new Properties();
        propiedades.load(new java.io.StringReader(applicationProperties()));

        assertThat(propiedades.getProperty("spring.datasource.url"))
                .startsWith("${DB_URL:")
                .contains("donaciones_db");
    }
}