package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MisionFactory: se rechaza el dato de entrada que no se puede usar")
class MisionFactoryTest {

    private final MisionFactory factory = new MisionFactory(new OperacionFactory());

    @Nested
    @DisplayName("constancia de tiempo")
    class Constancia {

        @ParameterizedTest
        @CsvSource({
                "MINUTOS, MINUTES",
                "HORAS, HOURS",
                "DIAS, DAYS",
                "SEMANAS, WEEKS",
                "MESES, MONTHS",
                "ANOS, YEARS"
        })
        @DisplayName("acepta las unidades en español, que es el idioma de la API")
        void aceptaLasUnidadesEnEspanol(String enviada, ChronoUnit esperada) {
            ReglaConstancia constancia = factory.crearConstancia(3, enviada);

            assertThat(constancia.getUnidadTiempo()).isEqualTo(esperada);
            assertThat(constancia.getCantidad()).isEqualTo(3);
        }

        @Test
        @DisplayName("acepta 'AÑOS' con tilde, que ChronoUnit.valueOf nunca reconoceria")
        void aceptaAniosConTilde() {
            assertThat(factory.crearConstancia(1, "AÑOS").getUnidadTiempo())
                    .isEqualTo(ChronoUnit.YEARS);
        }

        @Test
        @DisplayName("tolera espacios y minusculas")
        void toleraEspaciosYMinusculas() {
            assertThat(factory.crearConstancia(1, "  dias  ").getUnidadTiempo())
                    .isEqualTo(ChronoUnit.DAYS);
        }

        @Test
        @DisplayName("sigue aceptando los nombres en ingles que ya estan en la base")
        void sigueAceptandoLosNombresEnIngles() {
            assertThat(factory.crearConstancia(1, "MONTHS").getUnidadTiempo())
                    .isEqualTo(ChronoUnit.MONTHS);
        }

        @Test
        @DisplayName("si no viene cantidad o unidad, la mision no lleva constancia")
        void sinCantidadOUnidadNoHayConstancia() {
            assertThat(factory.crearConstancia(null, "MESES")).isNull();
            assertThat(factory.crearConstancia(3, null)).isNull();
            assertThat(factory.crearConstancia(3, "   ")).isNull();
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -2})
        @DisplayName("una cantidad en cero o negativa se rechaza")
        void rechazaCantidadNoPositiva(int cantidad) {
            assertThatThrownBy(() -> factory.crearConstancia(cantidad, "MESES"))
                    .isInstanceOf(DatosInvalidosException.class)
                    .hasMessageContaining("mayor a cero");
        }

        @ParameterizedTest
        @ValueSource(strings = {"NANOS", "FOREVER", "siglo", "SEMESTRALES"})
        @DisplayName("una unidad fuera de la lista admitida se rechaza y dice cuales valen")
        void rechazaUnidadNoAdmitida(String unidad) {
            assertThatThrownBy(() -> factory.crearConstancia(1, unidad))
                    .isInstanceOf(DatosInvalidosException.class)
                    .hasMessageContaining("MESES")
                    .hasMessageContaining("ANOS");
        }
    }

    @Nested
    @DisplayName("atributo de impacto")
    class Atributo {

        @ParameterizedTest
        @EnumSource(AtributoImpacto.class)
        @DisplayName("acepta todos los atributos del enum")
        void aceptaTodosLosAtributos(AtributoImpacto atributo) {
            assertThat(factory.crearAtributoImpacto(atributo.name())).isEqualTo(atributo);
        }

        @Test
        @DisplayName("tolera que el atributo venga en minusculas o con espacios")
        void toleraMinusculasYEspacios() {
            assertThat(factory.crearAtributoImpacto("categoria"))
                    .isEqualTo(AtributoImpacto.CATEGORIA);
            assertThat(factory.crearAtributoImpacto(" SUBCATEGORIA "))
                    .isEqualTo(AtributoImpacto.SUBCATEGORIA);
        }

        @Test
        @DisplayName("un atributo que no existe se rechaza enumerando los validos")
        void rechazaAtributoInexistente() {
            assertThatThrownBy(() -> factory.crearAtributoImpacto("PESO"))
                    .isInstanceOf(DatosInvalidosException.class)
                    .hasMessageContaining("PESO")
                    .hasMessageContaining("CANTIDAD_BIENES");
        }

        @Test
        @DisplayName("un atributo vacio o nulo se rechaza")
        void rechazaAtributoVacio() {
            assertThatThrownBy(() -> factory.crearAtributoImpacto("   "))
                    .isInstanceOf(DatosInvalidosException.class);
            assertThatThrownBy(() -> factory.crearAtributoImpacto(null))
                    .isInstanceOf(DatosInvalidosException.class);
        }
    }
}