package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.CategoriaDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.IntPredicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Una posición fuera de rango tiene que ser un error, no un pedido que se pierde en
 * silencio (punto 31).
 *
 * <p>El síntoma era invisible y por eso importa tanto. El gestor se salía sin hacer nada, el
 * caller igual escribía la posición pedida, y la respuesta al admin era un 200. La
 * secuencia quedaba con huecos, que es el invariante que {@code SecuenciaCategoria} declara
 * en su propio javadoc. Y el caso {@code posicionSecuencia: 0} era peor que un hueco: como
 * la categoría base del programa es la de posición más baja, <b>todos los donantes nuevos
 * pasaban a arrancar en esa categoría</b> en lugar de en la base.
 *
 * <p>Los casos concretos, sobre una secuencia {@code 1..5}:
 *
 * <ul>
 *   <li>Pedir la 10 con 5 categorías → queda {@code 1,2,3,4,5,10}, y un
 *       {@code desplazarHaciaArribaDesde(6)} posterior tampoco lo cierra.</li>
 *   <li>Pedir la 0 → la categoría queda en 0 y secuestra la categoría base.</li>
 * </ul>
 */
@DisplayName("Punto 31: una posición fuera de rango se rechaza en vez de perderse")
class SecuenciaCategoriaRangoTest {

    private final SecuenciaCategoria secuencia = new SecuenciaCategoria();

    /**
     * La secuencia como queda en la base: posición -> nombre de la categoría que la ocupa.
     *
     * <p>Se modela con nombres y no solo con posiciones porque un desplazamiento mueve
     * <em>filas</em>: si la categoría de la 3 sube a la 1, la 3 desaparece de la secuencia y
     * reaparece en la 1. Con una lista de enteros suelta, la 3 se duplicaría y el test
     * daría un falso negativo sobre la única propiedad que importa.
     *
     * <p>Es un {@code TreeMap} y no un {@code HashMap} porque el mapa tiene que seguir
     * ordenado por posición después de cada movimiento: si no, las aserciones comparan el
     * orden de inserción contra el orden real de la secuencia y fallan por un motivo que no
     * tiene que ver con lo que se está probando. Un descolador que metiera la categoría de
     * la 6 antes de la de la 1 no estaría fallando, solo desordenando.
     *
     * <p>Se mira el estado y no las llamadas porque lo que importa es que la secuencia
     * termine en {@code 1..N} sin huecos, que es el invariante roto. Con un {@code verify}
     * solo se comprobaría que el gestor llamó al {@code desplazar} correcto, no que el
     * resultado sea una secuencia válida.
     */
    private final Map<Integer, String> secuenciaEnBase = new TreeMap<>(Map.of(
            1, "A", 2, "B", 3, "C", 4, "D", 5, "E"));

    private final RepositorioCategorias repo = repositorioSimulado();

    /**
     * Un repositorio que aplica los desplazamientos de verdad sobre {@link #secuenciaEnBase}.
     *
     * <p>Es un mock con {@code doAnswer} y no una clase que implemente la interfaz porque
     * {@code JpaRepository} trae decenas de métodos, y escribirlos todos a mano para usar
     * cuatro es ruido que el compilador no deja distinguir de una implementación de
     * producción.
     */
    private RepositorioCategorias repositorioSimulado() {
        RepositorioCategorias simulado = mock(RepositorioCategorias.class);

        doAnswer(invoc -> {
            int inicio = invoc.getArgument(0);
            int fin = invoc.getArgument(1);
            correr(p -> p >= inicio && p <= fin, 1);
            return null;
        }).when(simulado).desplazarHaciaAbajo(anyInt(), anyInt());

        doAnswer(invoc -> {
            int inicio = invoc.getArgument(0);
            int fin = invoc.getArgument(1);
            correr(p -> p >= inicio && p <= fin, -1);
            return null;
        }).when(simulado).desplazarHaciaArriba(anyInt(), anyInt());

        doAnswer(invoc -> {
            int inicio = invoc.getArgument(0);
            correr(p -> p >= inicio, 1);
            return null;
        }).when(simulado).desplazarHaciaAbajoDesde(anyInt());

        when(simulado.listarPosiciones())
                .thenAnswer(invoc -> List.copyOf(secuenciaEnBase.keySet()));

        return simulado;
    }

    /**
     * Mueve todas las categorías cuya posición cumple el filtro, {@code delta} posiciones.
     *
     * <p>Las filas salen del mapa y vuelven a entrar, para que la operación sea simultánea
     * como el {@code UPDATE} de SQL: {@code SET posicion = posicion + 1 WHERE ...} calcula
     * el valor nuevo sobre el valor viejo de cada fila, así que mover la 2 a la 3 no pisa a la
     * que ya estaba en la 3.
     *
     * <p>Los pares se copian con {@code Map.entry} antes de remover nada, y no se itera
     * sobre el {@code entrySet()} del mapa: las entradas de un {@code TreeMap} son
     * <b>vivas</b>, o sea que al sacar una del árbol su {@code key} cambia al de la fila que
     * quedó en ese nodo. Con la lista de entradas vivas, el {@code remove} de una corrompía
     * las siguientes y el resultado del test era un mapa con huecos que no tenía nada que ver
     * con lo que se estaba probando.
     */
    private void correr(IntPredicate entra, int delta) {
        List<Map.Entry<Integer, String>> aMover = new ArrayList<>();

        secuenciaEnBase.forEach((posicion, nombre) -> {
            if (entra.test(posicion)) {
                aMover.add(Map.entry(posicion, nombre));
            }
        });

        aMover.forEach(entrada -> secuenciaEnBase.remove(entrada.getKey()));
        aMover.forEach(entrada -> secuenciaEnBase.put(entrada.getKey() + delta, entrada.getValue()));
    }

    /**
     * Los dos pasos que mueven una categoría, como los hace la aplicación.
     *
     * <p>No alcanza con llamar al gestor: {@code SecuenciaCategoria} solo deja libre la
     * posición y corre a las demás, y el guardado de la categoría en el destino lo hace
     * {@code CategoriaService} después, con un {@code moverAPosicion}. Los dos juntos son lo
     * que mueve la categoría, y probar solo el primero dejaría pasar el bug: el invariante
     * "sin huecos" es cosa de los dos.
     *
     * <p><b>Es todo o nada, y por eso el estado se restaura si el gestor rechaza.</b> En
     * producción es así porque los dos pasos corren dentro del mismo
     * {@code @Transactional}: si el gestor lanza, el rollback deja la secuencia como
     * estaba. Un helper que se comiera la categoría antes de validar y no la devolviera
     * haría fallar los tests de rechazo por un motivo que no es el del punto 31 —"la
     * secuencia quedó con un hueco"— sino uno inventado por el propio test.
     */
    private void moverCategoriaA(int posicionAnterior, int posicionNueva) {
        Map<Integer, String> antesDeMover = new TreeMap<>(secuenciaEnBase);
        String laQueSeMueve = secuenciaEnBase.remove(posicionAnterior);

        try {
            secuencia.desplazarParaActualizar(
                    repo, posicionAnterior, posicionNueva, antesDeMover.size());

            secuenciaEnBase.put(posicionNueva, comprobarQueEsta(laQueSeMueve, posicionAnterior));
        } catch (RuntimeException error) {
            secuenciaEnBase.clear();
            secuenciaEnBase.putAll(antesDeMover);
            throw error;
        }
    }

    /** Igual, pero en el alta: el gestor abre el hueco y después se guarda la nueva. */
    private void crearCategoriaEn(int posicion) {
        Map<Integer, String> antesDeCrear = new TreeMap<>(secuenciaEnBase);

        try {
            secuencia.desplazarParaCrear(repo, posicion, antesDeCrear.size());

            secuenciaEnBase.put(posicion, "NUEVA");
        } catch (RuntimeException error) {
            secuenciaEnBase.clear();
            secuenciaEnBase.putAll(antesDeCrear);
            throw error;
        }
    }

    private static String comprobarQueEsta(String nombre, int posicion) {
        assertThat(nombre)
                .as("la categoría de la posición %d tenía que estar en la secuencia", posicion)
                .isNotNull();
        return nombre;
    }

    /** Las posiciones de la secuencia, para poder comparar contra {@code 1..N}. */
    private List<Integer> posiciones() {
        return new ArrayList<>(secuenciaEnBase.keySet());
    }

    @Nested
    @DisplayName("el efecto observable: la secuencia nunca queda con huecos")
    class SecuenciaSinHuecos {

        @Test
        @DisplayName("pedir la 10 con 5 categorías es un 400 y la secuencia no se toca")
        void pedirLaDiezEsUnError() {
            assertThatThrownBy(() -> moverCategoriaA(2, 10))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(posiciones())
                    .as("lo importante: la secuencia quedó como estaba, no con un hueco")
                    .containsExactly(1, 2, 3, 4, 5);
        }

        @Test
        @DisplayName("pedir la 0 es un 400: era la que rompía la categoría base")
        void pedirLaCeroEsUnError() {
            assertThatThrownBy(() -> moverCategoriaA(2, 0))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(posiciones()).containsExactly(1, 2, 3, 4, 5);
        }

        @Test
        @DisplayName("subir dentro del rango sí desplaza y deja la secuencia completa")
        void subirDentroDelRangoDesplaza() {
            // La 3 sube a la 1: las que estaban en 1 y 2 bajan y la C queda en el lugar libre.
            moverCategoriaA(3, 1);

            assertThat(posiciones()).containsExactly(1, 2, 3, 4, 5);
            assertThat(secuenciaEnBase)
                    .as("y cada categoría sigue en la posición que le corresponde")
                    .containsExactly(
                            Map.entry(1, "C"), Map.entry(2, "A"), Map.entry(3, "B"),
                            Map.entry(4, "D"), Map.entry(5, "E"));
        }

        @Test
        @DisplayName("bajar dentro del rango sí desplaza y deja la secuencia completa")
        void bajarDentroDelRangoDesplaza() {
            // La 1 baja a la 4: las que estaban de la 2 a la 4 suben.
            moverCategoriaA(1, 4);

            assertThat(posiciones()).containsExactly(1, 2, 3, 4, 5);
            assertThat(secuenciaEnBase)
                    .containsExactly(
                            Map.entry(1, "B"), Map.entry(2, "C"), Map.entry(3, "D"),
                            Map.entry(4, "A"), Map.entry(5, "E"));
        }

        @Test
        @DisplayName("en la edición la 6 de 5 tampoco vale: dejaría un hueco")
        void enLaEdicionLaSextaNoVale() {
            // El número de categorías no cambia al editar, así que la 6 no existe. Admitirla
            // dejaba 1,_,3,4,5,6: la de la 2 se iba a la 6 y las del medio no corrían.
            assertThatThrownBy(() -> moverCategoriaA(2, 6))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(posiciones()).containsExactly(1, 2, 3, 4, 5);
        }

        @Test
        @DisplayName("quedarse en la misma posición no mueve nada")
        void mismaPosicionNoMueveNada() {
            moverCategoriaA(3, 3);

            assertThat(secuenciaEnBase)
                    .as("no tiene que pasar nada por la base: la secuencia queda igual")
                    .containsExactly(
                            Map.entry(1, "A"), Map.entry(2, "B"), Map.entry(3, "C"),
                            Map.entry(4, "D"), Map.entry(5, "E"));
        }
    }

    @Nested
    @DisplayName("el mismo control en el alta, no solo en la edición")
    class TambienEnElAlta {

        @Test
        @DisplayName("crear en la 10 con 5 categorías también es un error")
        void crearEnLaDiezEsUnError() {
            // El bug era idéntico en el alta: desplazarHaciaAbajoDesde(10) no movía nada y la
            // categoría se guardaba igual en la 10.
            assertThatThrownBy(() -> crearCategoriaEn(10))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(posiciones()).containsExactly(1, 2, 3, 4, 5);
        }

        @Test
        @DisplayName("crear en la 0 también es un error")
        void crearEnLaCeroEsUnError() {
            assertThatThrownBy(() -> crearCategoriaEn(0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("crear en una posición libre sí abre el hueco")
        void crearEnPosicionLibreAbreElHueco() {
            crearCategoriaEn(2);

            assertThat(posiciones()).containsExactly(1, 2, 3, 4, 5, 6);
        }

        @Test
        @DisplayName("crear al final no corre a nadie, y ahí el tope es max + 1")
        void crearAlFinalNoCorreANadie() {
            // En el alta la 6 sí es válida, porque la categoría nueva hace una más. Es la
            // diferencia con la edición, donde la 6 de 5 dejaría un hueco.
            crearCategoriaEn(6);

            assertThat(posiciones()).containsExactly(1, 2, 3, 4, 5, 6);
            assertThat(secuenciaEnBase)
                    .contains(Map.entry(6, "NUEVA"));
        }
    }

    @Nested
    @DisplayName("el mensaje dice cuál era la posición válida")
    class MensajeUtil {

        @Test
        @DisplayName("el error de fuera de rango nombra el rango válido")
        void elErrorNombraElRangoValido() {
            assertThatThrownBy(() -> secuencia.desplazarParaActualizar(repo, 2, 9, 5))
                    .hasMessageContaining("de 1 a 5");
        }

        @Test
        @DisplayName("en el alta el rango del mensaje llega hasta max + 1")
        void enElAltaElRangoLlegaHastaMaxMasUno() {
            assertThatThrownBy(() -> secuencia.desplazarParaCrear(repo, 9, 5))
                    .hasMessageContaining("de 1 a 6");
        }

        @Test
        @DisplayName("el error de la 0 explica que tiene que ser 1 o más")
        void elErrorDeLaCeroExplicaElMinimo() {
            assertThatThrownBy(() -> secuencia.desplazarParaActualizar(repo, 2, 0, 5))
                    .hasMessageContaining("1 o más");
        }
    }

    @Nested
    @DisplayName("el DTO rechaza el 0 antes de llegar al servicio")
    class ValidacionEnElDto {

        @Test
        @DisplayName("posicionSecuencia tiene @Min(1)")
        void posicionSecuenciaTieneMin() throws Exception {
            Field campo = CategoriaDTO.class.getDeclaredField("posicionSecuencia");
            Min min = campo.getAnnotation(Min.class);

            assertThat(min)
                    .as("""
                    Sin esto el 0 entra al servicio, pasa el chequeo de "ya está ocupada" y
                    sale guardada en la posición 0, que es justo la que secuestra la
                    categoría base.
                    """)
                    .isNotNull();
            assertThat(min.value()).isEqualTo(1);
        }

        @Test
        @DisplayName("el límite superior no se valida en el DTO porque depende de cuántas haya")
        void elLimiteSuperiorNoVaEnElDto() throws Exception {
            // Con 0 categorías la 1 es válida; con 5, la 10 ya no. El DTO no tiene forma de
            // saber cuántas hay, así que ese chequeo va en el gestor, que sí recibe el
            // repositorio.
            Field campo = CategoriaDTO.class.getDeclaredField("posicionSecuencia");

            assertThat(campo.getAnnotation(Max.class)).isNull();
            assertThat(campo.getAnnotation(Min.class).value()).isEqualTo(1);
        }
    }
}
