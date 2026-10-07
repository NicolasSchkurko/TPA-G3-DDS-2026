package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.ValoresDistintos;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Comparación de reglas")
class ReglaEquivalenciaTest {

    @Test
    @DisplayName("misma regla construida dos veces es equivalente")
    void mismaReglaEsEquivalente() {
        Regla una = new Regla(null, AtributoImpacto.ESTADO, new SuperaCantidad(5, 1));
        Regla otra = new Regla(null, AtributoImpacto.ESTADO, new SuperaCantidad(5, 1));

        assertThat(una.esEquivalenteA(otra)).isTrue();
    }

    @Test
    @DisplayName("cambiar el atributo que se mira no es equivalente")
    void cambiarElAtributoNoEsEquivalente() {
        Regla estado = new Regla(null, AtributoImpacto.ESTADO, new SuperaCantidad(5, 1));
        Regla categoria = new Regla(null, AtributoImpacto.CATEGORIA, new SuperaCantidad(5, 1));

        assertThat(estado.esEquivalenteA(categoria)).isFalse();
    }

    @Test
    @DisplayName("agregar o quitar la constancia no es equivalente")
    void cambiarLaConstanciaNoEsEquivalente() {
        Regla sinConstancia = new Regla(null, AtributoImpacto.ESTADO, new SuperaCantidad(5, 1));
        Regla conConstancia = new Regla(
                new ReglaConstancia(1, ChronoUnit.MONTHS),
                AtributoImpacto.ESTADO,
                new SuperaCantidad(5, 1)
        );

        assertThat(sinConstancia.esEquivalenteA(conConstancia)).isFalse();
        assertThat(conConstancia.esEquivalenteA(sinConstancia)).isFalse();
    }

    @Test
    @DisplayName("misma constancia es equivalente aunque sea otro objeto")
    void mismaConstanciaEsEquivalente() {
        // La constancia se compara campo por campo: equals no está definido en la entidad.
        Regla una = new Regla(new ReglaConstancia(2, ChronoUnit.WEEKS),
                AtributoImpacto.ESTADO, new SuperaCantidad(5, 1));
        Regla otra = new Regla(new ReglaConstancia(2, ChronoUnit.WEEKS),
                AtributoImpacto.ESTADO, new SuperaCantidad(5, 1));

        assertThat(una.esEquivalenteA(otra)).isTrue();
    }

    @Test
    @DisplayName("cambiar la cantidad o la unidad de la constancia no es equivalente")
    void cambiarLaConstanciaEnSiNoEsEquivalente() {
        Regla dosMeses = new Regla(new ReglaConstancia(2, ChronoUnit.MONTHS),
                AtributoImpacto.ESTADO, new SuperaCantidad(5, 1));
        Regla dosSemanas = new Regla(new ReglaConstancia(2, ChronoUnit.WEEKS),
                AtributoImpacto.ESTADO, new SuperaCantidad(5, 1));
        Regla tresMeses = new Regla(new ReglaConstancia(3, ChronoUnit.MONTHS),
                AtributoImpacto.ESTADO, new SuperaCantidad(5, 1));

        assertThat(dosMeses.esEquivalenteA(dosSemanas)).isFalse();
        assertThat(dosMeses.esEquivalenteA(tresMeses)).isFalse();
    }

    @Test
    @DisplayName("cambiar de tipo de operacion no es equivalente")
    void cambiarDeOperacionNoEsEquivalente() {
        Regla una = new Regla(null, AtributoImpacto.CATEGORIA, new SuperaCantidad(5, 1));
        Regla otra = new Regla(null, AtributoImpacto.CATEGORIA, new ValoresDistintos(5, 1));

        assertThat(una.esEquivalenteA(otra)).isFalse();
    }

    @Test
    @DisplayName("ninguna regla es equivalente a null")
    void ningunaReglaEsEquivalenteANull() {
        Regla regla = new Regla(null, AtributoImpacto.ESTADO, new SuperaCantidad(5, 1));

        assertThat(regla.esEquivalenteA(null)).isFalse();
    }
}
