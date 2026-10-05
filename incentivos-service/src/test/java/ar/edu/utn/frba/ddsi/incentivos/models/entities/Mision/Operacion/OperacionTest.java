package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.ValoresDistintos;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Motor de operaciones de regla (patrón Strategy)")
class OperacionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Donante de mentira: guarda lo que la operación le registra, que es justamente lo que
     * hace un {@code ProgresoMision}. Sirve para probar que cada donante lleva su propia
     * cuenta.
     */
    private static ProgresoDelDonante donante() {
        return new ProgresoDelDonante() {
            private final List<String> valores = new ArrayList<>();

            @Override
            public boolean registrarValorObservado(String valor) {
                if (valores.contains(valor)) {
                    return false;
                }
                valores.add(valor);
                return true;
            }

            @Override
            public int cantidadValoresObservados() {
                return valores.size();
            }
        };
    }

    @Nested
    @DisplayName("Operacion base")
    class Base {

        @Test
        @DisplayName("completa cuando el progreso alcanza el objetivo")
        void completaAlAlcanzarElObjetivo() {
            SuperaCantidad operacion = new SuperaCantidad(3, 1);

            assertThat(operacion.estaCompleta(2, donante())).isFalse();
            assertThat(operacion.estaCompleta(3, donante())).isTrue();
            assertThat(operacion.estaCompleta(4, donante())).isTrue();
        }

        @Test
        @DisplayName("no completa si el progreso o el objetivo son nulos")
        void noCompletaConValoresNulos() {
            SuperaCantidad sinObjetivo = new SuperaCantidad(null, 1);

            assertThat(sinObjetivo.estaCompleta(5, donante())).isFalse();
            assertThat(new SuperaCantidad(3, 1).estaCompleta(null, donante())).isFalse();
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

            assertThat(operacion.calcularProgreso("ENTREGADA", donante())).isTrue();
            assertThat(operacion.calcularProgreso("RECIBIDA", donante())).isFalse();
        }

        @Test
        @DisplayName("ignora mayúsculas y espacios del valor recibido")
        void esToleranteAlFormatoDelValor() {
            CantidadCoincidencias operacion =
                    new CantidadCoincidencias(1, MAPPER.valueToTree("ENTREGADA"));

            assertThat(operacion.calcularProgreso("  entregada ", donante())).isTrue();
        }

        @Test
        @DisplayName("no progresa con valor o esperado nulo")
        void noProgresaConNulos() {
            CantidadCoincidencias sinEsperado =
                    new CantidadCoincidencias(1, null);
            CantidadCoincidencias conEsperado =
                    new CantidadCoincidencias(1, MAPPER.valueToTree("ENTREGADA"));

            assertThat(sinEsperado.calcularProgreso("ENTREGADA", donante())).isFalse();
            assertThat(conEsperado.calcularProgreso(null, donante())).isFalse();
        }
    }

    @Nested
    @DisplayName("SuperaCantidad")
    class SuperaCantidadTest {

        @Test
        @DisplayName("progresa cuando el valor iguala o supera el mínimo")
        void comparaContraElMinimo() {
            SuperaCantidad operacion = new SuperaCantidad(1, 6);

            assertThat(operacion.calcularProgreso(7, donante())).isTrue();
            assertThat(operacion.calcularProgreso(6, donante())).isTrue();
            assertThat(operacion.calcularProgreso(5, donante())).isFalse();
        }

        @Test
        @DisplayName("no progresa si el valor no es numérico")
        void ignoraValoresNoNumericos() {
            SuperaCantidad operacion = new SuperaCantidad(1, 6);

            assertThat(operacion.calcularProgreso("muchos", donante())).isFalse();
            assertThat(operacion.calcularProgreso(null, donante())).isFalse();
        }
    }

    @Nested
    @DisplayName("ValoresDistintos")
    class ValoresDistintosTest {

        @Test
        @DisplayName("acumula en el donante los valores distintos, sin repetir")
        void acumulaValoresEnElDonante() {
            ValoresDistintos operacion = new ValoresDistintos(3, 2);
            ProgresoDelDonante donante = donante();

            assertThat(operacion.calcularProgreso("Ropa", donante)).isTrue();
            assertThat(operacion.calcularProgreso("Ropa", donante)).isTrue();
            assertThat(operacion.calcularProgreso("Alimento", donante)).isTrue();

            assertThat(donante.cantidadValoresObservados()).isEqualTo(2);
        }

        @Test
        @DisplayName("el mismo valor repetido no cuenta como valor nuevo")
        void unValorRepetidoNoEsNuevo() {
            ValoresDistintos operacion = new ValoresDistintos(3, 2);
            ProgresoDelDonante donante = donante();

            assertThat(donante.registrarValorObservado("Ropa")).isTrue();
            assertThat(operacion.calcularProgreso("Ropa", donante)).isTrue();
            assertThat(operacion.calcularProgreso("Ropa", donante)).isTrue();

            assertThat(donante.cantidadValoresObservados()).isEqualTo(1);
        }

        @Test
        @DisplayName("no completa hasta cumplir el objetivo y la cantidad de valores distintos")
        void exigeAmbasCondiciones() {
            ValoresDistintos operacion = new ValoresDistintos(2, 2);
            ProgresoDelDonante donante = donante();

            operacion.calcularProgreso("Ropa", donante);
            assertThat(operacion.estaCompleta(2, donante)).isFalse();

            operacion.calcularProgreso("Alimento", donante);
            assertThat(operacion.estaCompleta(2, donante)).isTrue();
        }

        @Test
        @DisplayName("no completa si el progreso es nulo")
        void noCompletaConProgresoNulo() {
            ValoresDistintos operacion = new ValoresDistintos(2, 2);
            ProgresoDelDonante donante = donante();

            operacion.calcularProgreso("Ropa", donante);
            operacion.calcularProgreso("Alimento", donante);

            assertThat(operacion.estaCompleta(null, donante)).isFalse();
        }

        @Test
        @DisplayName("una donación sin el atributo no cuenta ni infla el conteo")
        void unaDonacionSinElAtributoNoAporta() {
            ValoresDistintos operacion = new ValoresDistintos(1, 2);
            ProgresoDelDonante donante = donante();

            // Antes MAPPER.valueToTree(null) devolvia null y ese null se agregaba a la
            // lista, con lo que una sola donacion sin categoria ya contaba como valor.
            assertThat(operacion.calcularProgreso(null, donante)).isFalse();

            assertThat(donante.cantidadValoresObservados()).isZero();
            assertThat(operacion.estaCompleta(1, donante)).isFalse();
        }

        @Test
        @DisplayName("un donante no hereda los valores que vio otro en la misma misión")
        void losDonantesNoCompartenValores() {
            ValoresDistintos operacion = new ValoresDistintos(1, 2);

            ProgresoDelDonante ana = donante();
            ProgresoDelDonante beto = donante();

            // Ana dona Ropa, Beto dona Alimentos y Muebles: el avance de Beto no puede
            // completar la regla de Ana.
            operacion.calcularProgreso("Ropa", ana);
            operacion.calcularProgreso("Alimentos", beto);
            operacion.calcularProgreso("Muebles", beto);

            assertThat(beto.cantidadValoresObservados()).isEqualTo(2);
            assertThat(ana.cantidadValoresObservados()).isEqualTo(1);

            assertThat(operacion.estaCompleta(1, beto)).isTrue();
            assertThat(operacion.estaCompleta(1, ana)).isFalse();
        }

        @Test
        @DisplayName("la operación no guarda estado propio: es solo configuración")
        void laOperacionNoGuardaEstadoPropio() {
            ValoresDistintos operacion = new ValoresDistintos(1, 2);
            ProgresoDelDonante donante = donante();

            operacion.calcularProgreso("Ropa", donante);

            // Si la operación volviera a tener su propia lista, esto seria 1 y el bug
            // del estado compartido volvdría a aparecer.
            assertThat(operacion.estaCompleta(1, donante())).isFalse();
        }
    }
}