package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La secuencia de posiciones de las categorías (punto 19).
 */
@DisplayName("SecuenciaCategoria")
class SecuenciaCategoriaTest {

    private final RepositorioCategorias repo = mock(RepositorioCategorias.class);
    private final SecuenciaCategoria secuencia = new SecuenciaCategoria();

    @Test
    @DisplayName("crear en una posición baja todas las que están desde ahí para arriba")
    void crearDesplazaHaciaAbajoDesdeLaPosicionNueva() {
        // Con 10 categorías, meter una en la 3 tiene que correr a las que estaban de la 3
        // en adelante. El máximo va porque el gestor valida el rango (punto 31).
        secuencia.desplazarParaCrear(repo, 3, 10);

        verify(repo).desplazarHaciaAbajoDesde(3);
    }

    @Test
    @DisplayName("crear sin posición no toca nada")
    void crearSinPosicionNoTocaNada() {
        secuencia.desplazarParaCrear(repo, null, 10);

        verify(repo, never()).desplazarHaciaAbajoDesde(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("crear al final de la secuencia no corre a nadie")
    void crearAlFinalNoCorreANadie() {
        // max + 1 es la posición siguiente a la última: poner la categoría al final del
        // programa es legítimo y no necesita mover nada.
        secuencia.desplazarParaCrear(repo, 6, 5);

        verify(repo, never()).desplazarHaciaAbajoDesde(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("borrar deja la secuencia sin huecos")
    void borrarDejaLaSecuenciaSinHuecos() {
        secuencia.desplazarParaEliminar(repo, 2);

        verify(repo).desplazarHaciaArribaDesde(3);
    }

    @Test
    @DisplayName("subir de posición baja a las que estaban en el medio")
    void subirDesplazaHaciaAbajoElRangoIntermedio() {
        // De la 5 a la 2: las que estaban en 2, 3 y 4 tienen que bajar un lugar.
        secuencia.desplazarParaActualizar(repo, 5, 2, 10);

        verify(repo).desplazarHaciaAbajo(2, 4);
        verify(repo, never()).desplazarHaciaArriba(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("bajar de posición sube a las que estaban en el medio")
    void bajarDesplazaHaciaArribaElRangoIntermedio() {
        // De la 2 a la 5: las que estaban en 3, 4 y 5 tienen que subir un lugar.
        secuencia.desplazarParaActualizar(repo, 2, 5, 10);

        verify(repo).desplazarHaciaArriba(3, 5);
        verify(repo, never()).desplazarHaciaAbajo(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("quedarse en la misma posición no desplaza nada")
    void mismaPosicionNoDesplazaNada() {
        secuencia.desplazarParaActualizar(repo, 3, 3, 10);

        verify(repo, never()).desplazarHaciaAbajo(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(repo, never()).desplazarHaciaArriba(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("ir más allá de la última posición es un error, no un 'no hacer nada'")
    void masAllaDeLaUltimaPosicionEsUnError() {
        // Con 5 categorías la 10 no existe. Antes el gestor se salía en silencio y el caller
        // igual escribía la 10, dejando la secuencia 1,2,3,4,5,10 con un hueco (punto 31).
        assertThatThrownBy(() -> secuencia.desplazarParaActualizar(repo, 2, 10, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10")
                .hasMessageContaining("fuera de rango");

        verify(repo, never()).desplazarHaciaAbajo(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(repo, never()).desplazarHaciaArriba(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("la posición 0 es un error: era la que rompía la categoría base")
    void laPosicionCeroEsUnError() {
        // La categoría base es la de posición más baja, así que una categoría en 0 hacía que
        // todos los donantes nuevos arrancaran en ella.
        assertThatThrownBy(() -> secuencia.desplazarParaActualizar(repo, 2, 0, 5))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> secuencia.desplazarParaCrear(repo, 0, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ir al final de la secuencia en la edición es un error: dejaría un hueco")
    void irAlFinalEnLaEdicionEsUnError() {
        // Con 5 categorías la 6 no existe. Admitirla dejaba la secuencia 1,_,3,4,5,6.
        // En el alta sí es válida, porque la categoría nueva hace una más.
        assertThatThrownBy(() -> secuencia.desplazarParaActualizar(repo, 2, 6, 5))
                .isInstanceOf(IllegalArgumentException.class);

        verify(repo, never()).desplazarHaciaAbajo(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(repo, never()).desplazarHaciaArriba(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("el mensaje dice cuál era la posición válida")
    void elMensajeDiceLaPosicionValida() {
        assertThatThrownBy(() -> secuencia.desplazarParaActualizar(repo, 2, 9, 5))
                .hasMessageContaining("de 1 a 5");
    }

    @Test
    @DisplayName("sin categorías con posición, la única válida es la 1")
    void sinCategoriasLaUnicaValidaEsLaUno() {
        // La primera categoría del programa entra en la 1 y no tiene a quién correr: ya está
        // al final. Lo que no se puede es pedir la 2.
        secuencia.desplazarParaCrear(repo, 1, null);

        verify(repo, never()).desplazarHaciaAbajoDesde(org.mockito.ArgumentMatchers.any());

        assertThatThrownBy(() -> secuencia.desplazarParaCrear(repo, 2, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("la posición máxima sale de las posiciones reales, no de cuántas hay")
    void laPosicionMaximaSaleDeLasPosicionesReales() {
        // Con un hueco (1, 2, 7) el limite correcto es 7. Con count() seria 3, y al
        // desplazar hacia una posicion mayor que 3 no moveria nada cuando si deberia.
        when(repo.listarPosiciones()).thenReturn(List.of(1, 2, 7));

        assertThat(secuencia.posicionMaxima(repo)).isEqualTo(7);
    }

    @Test
    @DisplayName("sin categorías no hay posición máxima")
    void sinCategoriasNoHayPosicionMaxima() {
        when(repo.listarPosiciones()).thenReturn(List.of());

        assertThat(secuencia.posicionMaxima(repo)).isNull();
    }
}
