package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.ValoresDistintos;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Qué cambios en una regla invalidan el progreso ya acumulado, y cuáles no (punto 15).
 *
 * <p>Esto es lo que evita que retocar el texto de una misión le borre el avance a todos
 * los que estaban por completarla.
 */
@DisplayName("Comparación de operaciones: qué cuenta como cambio de verdad")
class OperacionEquivalenciaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("Operacion base")
    class Base {

        @Test
        @DisplayName("dos operaciones idénticas son equivalentes")
        void identicasSonEquivalentes() {
            assertThat(new SuperaCantidad(3, 5).esEquivalenteA(new SuperaCantidad(3, 5))).isTrue();
        }

        @Test
        @DisplayName("cambiar el objetivo no es equivalente")
        void cambiarElObjetivoNoEsEquivalente() {
            assertThat(new SuperaCantidad(3, 5).esEquivalenteA(new SuperaCantidad(4, 5))).isFalse();
        }

        @Test
        @DisplayName("cambiar el tipo de operación no es equivalente")
        void cambiarElTipoNoEsEquivalente() {
            assertThat(new SuperaCantidad(3, 5)
                    .esEquivalenteA(new ValoresDistintos(3, 5))).isFalse();
        }

        @Test
        @DisplayName("nada es equivalente a null")
        void nadaEsEquivalenteANull() {
            assertThat(new SuperaCantidad(3, 5).esEquivalenteA(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("SuperaCantidad")
    class SuperaCantidadTest {

        @Test
        @DisplayName("subir el minimo NO invalida lo ya acreditado")
        void subirElMinimoNoInvalidaLoAcreditado() {
            // Una donación de 5 bienes contaba con mínimo 4 y sigue contando con mínimo 6.
            // Si esto devolviera false, un retoque en el umbral les borraría el progreso a
            // todos los que estaban en la misión.
            assertThat(new SuperaCantidad(5, 4)
                    .esEquivalenteA(new SuperaCantidad(5, 6))).isTrue();
        }
    }

    @Nested
    @DisplayName("ValoresDistintos")
    class ValoresDistintosTest {

        @Test
        @DisplayName("cambiar cuantos valores se piden SI invalida lo acumulado")
        void cambiarLaCantidadDeValoresSiInvalida() {
            assertThat(new ValoresDistintos(6, 3)
                    .esEquivalenteA(new ValoresDistintos(6, 4))).isFalse();
        }

        @Test
        @DisplayName("misma cantidad de valores y mismo objetivo es equivalente")
        void mismaCantidadEsEquivalente() {
            assertThat(new ValoresDistintos(6, 3)
                    .esEquivalenteA(new ValoresDistintos(6, 3))).isTrue();
        }
    }

    @Nested
    @DisplayName("CantidadCoincidencias")
    class CantidadCoincidenciasTest {

        @Test
        @DisplayName("cambiar el valor esperado SI invalida lo acumulado")
        void cambiarElValorEsperadoSiInvalida() {
            assertThat(new CantidadCoincidencias(5, MAPPER.valueToTree("ENTREGADA"))
                    .esEquivalenteA(new CantidadCoincidencias(5, MAPPER.valueToTree("RECIBIDA"))))
                    .isFalse();
        }

        @Test
        @DisplayName("el mismo valor esperado con otro objeto JsonNode es equivalente")
        void mismoValorEsperadoEsEquivalente() {
            // Dos TextNode distintos en memoria pero con el mismo texto: el equals de
            // JsonNode compara estructura, no referencia. Si comparara referencia, cada
            // edicion de la misión pareceria un cambio y se perderia todo el progreso.
            assertThat(new CantidadCoincidencias(5, MAPPER.valueToTree("ENTREGADA"))
                    .esEquivalenteA(new CantidadCoincidencias(5, MAPPER.valueToTree("ENTREGADA"))))
                    .isTrue();
        }
    }
}
