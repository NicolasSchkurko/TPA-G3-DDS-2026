package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.ValoresDistintos;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Motor de operaciones de regla (patrón Strategy)")
class OperacionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("Operacion base")
    class Base {

        @Test
        @DisplayName("completa cuando el progreso alcanza el objetivo")
        void completaAlAlcanzarElObjetivo() {
            SuperaCantidad operacion = new SuperaCantidad(3, 1);

            assertThat(operacion.estaCompleta(2)).isFalse();
            assertThat(operacion.estaCompleta(3)).isTrue();
            assertThat(operacion.estaCompleta(4)).isTrue();
        }

        @Test
        @DisplayName("no completa si el progreso o el objetivo son nulos")
        void noCompletaConValoresNulos() {
            SuperaCantidad sinObjetivo = new SuperaCantidad(null, 1);

            assertThat(sinObjetivo.estaCompleta(5)).isFalse();
            assertThat(new SuperaCantidad(3, 1).estaCompleta(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("CantidadCoincidencias")
    class Coincidencias {

        @Test
        @DisplayName("progresa solo si el valor coincide con el esperado")
        void comparaConElValorEsperado() {
            CantidadCoincidencias operacion =
                    new CantidadCoincidencias(1, MAPPER.valueToTree("ENTREGADA"));

            assertThat(operacion.calcularProgreso("ENTREGADA")).isTrue();
            assertThat(operacion.calcularProgreso("RECIBIDA")).isFalse();
        }

        @Test
        @DisplayName("ignora mayúsculas y espacios del valor recibido")
        void esToleranteAlFormatoDelValor() {
            CantidadCoincidencias operacion =
                    new CantidadCoincidencias(1, MAPPER.valueToTree("ENTREGADA"));

            assertThat(operacion.calcularProgreso("  entregada ")).isTrue();
        }

        @Test
        @DisplayName("no progresa con valor o esperado nulo")
        void noProgresaConNulos() {
            CantidadCoincidencias sinEsperado =
                    new CantidadCoincidencias(1, null);
            CantidadCoincidencias conEsperado =
                    new CantidadCoincidencias(1, MAPPER.valueToTree("ENTREGADA"));

            assertThat(sinEsperado.calcularProgreso("ENTREGADA")).isFalse();
            assertThat(conEsperado.calcularProgreso(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("SuperaCantidad")
    class SuperaCantidadTest {

        @Test
        @DisplayName("progresa cuando el valor iguala o supera el mínimo")
        void comparaContraElMinimo() {
            SuperaCantidad operacion = new SuperaCantidad(1, 6);

            assertThat(operacion.calcularProgreso(7)).isTrue();
            assertThat(operacion.calcularProgreso(6)).isTrue();
            assertThat(operacion.calcularProgreso(5)).isFalse();
        }

        @Test
        @DisplayName("no progresa si el valor no es numérico")
        void ignoraValoresNoNumericos() {
            SuperaCantidad operacion = new SuperaCantidad(1, 6);

            assertThat(operacion.calcularProgreso("muchos")).isFalse();
            assertThat(operacion.calcularProgreso(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("ValoresDistintos")
    class ValoresDistintosTest {

        @Test
        @DisplayName("acumula valores distintos sin repetir los ya vistos")
        void acumulaValoresSinDuplicar() {
            ValoresDistintos operacion = new ValoresDistintos(3, 2);

            assertThat(operacion.calcularProgreso("Ropa")).isTrue();
            assertThat(operacion.calcularProgreso("Ropa")).isTrue();
            assertThat(operacion.calcularProgreso("Alimento")).isTrue();

            assertThat(operacion.getValoresDistintos()).hasSize(2);
        }

        @Test
        @DisplayName("no completa hasta cumplir el objetivo y la cantidad de valores distintos")
        void exigeAmbasCondiciones() {
            ValoresDistintos operacion = new ValoresDistintos(2, 2);

            operacion.calcularProgreso("Ropa");
            assertThat(operacion.estaCompleta(2)).isFalse();

            operacion.calcularProgreso("Alimento");
            assertThat(operacion.estaCompleta(2)).isTrue();
        }

        @Test
        @DisplayName("no completa si el progreso es nulo")
        void noCompletaConProgresoNulo() {
            ValoresDistintos operacion = new ValoresDistintos(2, 2);
            operacion.calcularProgreso("Ropa");
            operacion.calcularProgreso("Alimento");

            assertThat(operacion.estaCompleta(null)).isFalse();
        }
    }
}