package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.ProgresoDelDonante;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.ValoresDistintos;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OperacionFactory: una operacion incompleta no se puede persistir")
class OperacionFactoryTest {

    private final OperacionFactory factory = new OperacionFactory();

    /** Las operaciones reciben el avance del donante; aca ninguno lo necesita. */
    private static ProgresoDelDonante donante() {
        return new ProgresoMision(null);
    }

    @Test
    @DisplayName("COINCIDENCIAS sin valor esperado armaba una mision imposible de completar")
    void coincidenciasSinValorEsperadoSeRechaza() {
        assertThatThrownBy(() -> factory.conseguirOperacion("COINCIDENCIAS", 3, null, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("valorEsperado");
    }

    @Test
    @DisplayName("COINCIDENCIAS con valor esperado guarda el valor con el que compara")
    void coincidenciasConValorEsperadoSeArma() {
        Operacion operacion = factory.conseguirOperacion("COINCIDENCIAS", 3, null, "ENTREGADA");

        assertThat(operacion).isInstanceOf(CantidadCoincidencias.class);
        assertThat(operacion.getProgresoObjetivo()).isEqualTo(3);
        assertThat(((CantidadCoincidencias) operacion).getValorEsperado().asText())
                .isEqualTo("ENTREGADA");
    }

    @Test
    @DisplayName("SUPERA_CANTIDAD sin cantidad reventaba con NPE al llegar una donacion")
    void superaCantidadSinCantidadSeRechaza() {
        assertThatThrownBy(() -> factory.conseguirOperacion("SUPERA_CANTIDAD", 3, null, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("cantidad");
    }

    @Test
    @DisplayName("SUPERA_CANTIDAD guarda el umbral que debe superar la donacion")
    void superaCantidadSeArma() {
        Operacion operacion = factory.conseguirOperacion("SUPERA_CANTIDAD", 2, 5, null);

        assertThat(operacion).isInstanceOf(SuperaCantidad.class);
        assertThat(((SuperaCantidad) operacion).getCantidadEsperada()).isEqualTo(5);
        assertThat(operacion.calcularProgreso(4, donante())).isFalse();
        assertThat(operacion.calcularProgreso(5, donante())).isTrue();
    }

    @Test
    @DisplayName("VALORES_DISTINTOS necesita saber cuantos valores distintos exige")
    void valoresDistintosSinCantidadSeRechaza() {
        assertThatThrownBy(() -> factory.conseguirOperacion("VALORES_DISTINTOS", 3, null, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("cantidad");
    }

    @Test
    @DisplayName("VALORES_DISTINTOS guarda cuantos valores diferentes busca")
    void valoresDistintosSeArma() {
        Operacion operacion = factory.conseguirOperacion("VALORES_DISTINTOS", 2, 3, null);

        assertThat(operacion).isInstanceOf(ValoresDistintos.class);
        assertThat(((ValoresDistintos) operacion).getCantValoresDistintos()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("una cantidad en cero o negativa se rechaza")
    void rechazaCantidadNoPositiva(int cantidad) {
        assertThatThrownBy(() -> factory.conseguirOperacion("SUPERA_CANTIDAD", 3, cantidad, null))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(
                () -> factory.conseguirOperacion("VALORES_DISTINTOS", 3, cantidad, null))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    @DisplayName("un tipo de operacion desconocido se rechaza enumerando los validos")
    void rechazaTipoDesconocido() {
        assertThatThrownBy(() -> factory.conseguirOperacion("CUALQUIER_COSA", 3, 1, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("CUALQUIER_COSA")
                .hasMessageContaining("COINCIDENCIAS");
    }

    @Test
    @DisplayName("un tipo vacio o nulo se rechaza")
    void rechazaTipoVacio() {
        assertThatThrownBy(() -> factory.conseguirOperacion("  ", 3, 1, null))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> factory.conseguirOperacion(null, 3, 1, null))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -4})
    @DisplayName("el progreso objetivo tiene que ser mayor a cero")
    void rechazaProgresoObjetivoNoPositivo(int progresoObjetivo) {
        assertThatThrownBy(
                () -> factory.conseguirOperacion("SUPERA_CANTIDAD", progresoObjetivo, 2, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("progreso objetivo");
    }

    @Test
    @DisplayName("el tipo de operacion no distingue mayusculas")
    void elTipoNoDistingueMayusculas() {
        assertThat(factory.conseguirOperacion("supera_cantidad", 1, 1, null))
                .isInstanceOf(SuperaCantidad.class);
    }
}