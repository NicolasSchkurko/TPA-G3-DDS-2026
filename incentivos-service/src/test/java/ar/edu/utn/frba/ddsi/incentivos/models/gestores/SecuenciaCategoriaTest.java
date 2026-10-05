package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import static org.assertj.core.api.Assertions.assertThat;
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
        secuencia.desplazarParaCrear(repo, 3);

        verify(repo).desplazarHaciaAbajoDesde(3);
    }

    @Test
    @DisplayName("crear sin posición no toca nada")
    void crearSinPosicionNoTocaNada() {
        secuencia.desplazarParaCrear(repo, null);

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
    @DisplayName("ir más allá de la última posición no desplaza nada")
    void masAllaDeLaUltimaPosicionNoDesplaza() {
        // No hay nada en la posición 10, asi que no se puede ir ahi.
        secuencia.desplazarParaActualizar(repo, 2, 10, 5);

        verify(repo, never()).desplazarHaciaAbajo(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(repo, never()).desplazarHaciaArriba(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
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
