package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

@DisplayName("SecuenciaCategoria")
class SecuenciaCategoriaTest {
        @Nested
        @DisplayName("SecuenciaCategoria")
        class Operaciones {


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
            // Con 5 categorías la 10 no existe: antes se guardaba igual, dejando un hueco.
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
            // Con 5 categorías la 6 no existe al editar; en el alta sí vale.
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

        @Test
        @DisplayName("una categoría sin posición se incorpora como un alta")
        void categoriaSinPosicionSeIncorporaComoAlta() {
            // Una categoría creada sin posición no ocupa un lugar en la secuencia. Al moverla
            // hay que abrirle el hueco, no pisar a la que ya estaba en el destino.
            secuencia.desplazarParaActualizar(repo, null, 2, 3);

            verify(repo).desplazarHaciaAbajoDesde(2);
        }

        @Test
        @DisplayName("sin posición, el final de la secuencia no corre a nadie")
        void sinPosicionAlFinalNoCorreANadie() {
            // Con 3 categorías la 4 es el final: entra sin desplazar.
            secuencia.desplazarParaActualizar(repo, null, 4, 3);

            verify(repo, never()).desplazarHaciaAbajoDesde(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("sin posición, una posición fuera de rango es un error")
        void sinPosicionFueraDeRangoEsUnError() {
            assertThatThrownBy(() -> secuencia.desplazarParaActualizar(repo, null, 5, 3))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        }

        @Nested
        @DisplayName("Punto 31: una posición fuera de rango se rechaza en vez de perderse")
        class Rangos {


        private final SecuenciaCategoria secuencia = new SecuenciaCategoria();

        /** La secuencia como queda en la base: posición -> nombre de la categoría que la ocupa. */
        private final Map<Integer, String> secuenciaEnBase = new TreeMap<>(Map.of(
                1, "A", 2, "B", 3, "C", 4, "D", 5, "E"));

        private final RepositorioCategorias repo = repositorioSimulado();

        /** Un repositorio que aplica los desplazamientos de verdad sobre {@link #secuenciaEnBase}. */
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

        /** Mueve las categorías que cumplen el filtro {@code delta} posiciones, como un UPDATE
         * simultáneo. Se copian los pares antes de remover: las entradas de un TreeMap son vivas. */
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

        /** Los dos pasos que mueven una categoría: el gestor abre el hueco y CategoriaService la
         * guarda en el destino. Si el gestor rechaza se restaura el estado, como el rollback. */
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
                // La 6 no existe al editar: admitirla dejaba 1,_,3,4,5,6.
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
                // El mismo bug en el alta: desplazarHaciaAbajoDesde(10) no movía nada.
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
                // En el alta la 6 sí vale: la categoría nueva hace una más.
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
                // El tope depende de cuántas categorías haya: se valida en el gestor, no en el DTO.
                Field campo = CategoriaDTO.class.getDeclaredField("posicionSecuencia");

                assertThat(campo.getAnnotation(Max.class)).isNull();
                assertThat(campo.getAnnotation(Min.class).value()).isEqualTo(1);
            }
        }
        }

}
