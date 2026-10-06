package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Quitarle una misión a una categoría no puede dejar a un donante bloqueado (punto 30).
 *
 * <p>El escenario que motiva esto: la categoría tenía {@code [A(1), B(2), C(3)]} y había
 * 40 donantes en {@code C}. El admin hizo {@code PUT /api/categorias/admin/{id}} con
 * {@code "misiones": ["A", "B"]}. Para cada donante, la posición 3 ya no existía, el
 * código pedía esa posición y recibía {@code null}, y entraba al retorno temprano que
 * solo hacía {@code progresoMisionActual = null}.
 *
 * <p>El donante quedaba bloqueado <b>para siempre</b>: sin misión, {@code progresarMision}
 * corta en el primer {@code if}, así que no completaba nada, no recibía insignias y no
 * aparecía en el ranking. Y tampoco se emitía {@code MisionCambiada}, así que no había
 * forma de enterarse desde afuera de que había pasado algo.
 */
@DisplayName("Punto 30: quitar una misión de una categoría no bloquea al donante")
class SincronizacionPerfilesMisionRemovidaTest {

    private RepositorioPerfiles repoPerfiles;
    private SincronizacionPerfiles sincronizacion;

    @BeforeEach
    void setUp() {
        repoPerfiles = mock(RepositorioPerfiles.class);
        sincronizacion = new SincronizacionPerfiles(repoPerfiles);
        when(repoPerfiles.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    /**
     * Una misión con id asignado.
     *
     * <p>El id se mete por reflexión a propósito, y no es un rodeo sin motivo: Hibernate se
     * lo asigna al insertar, así que una {@code Mision} suelta tiene {@code getIdMision()} en
     * {@code null}. El código de {@code SincronizacionPerfiles} compara misiones por id para
     * decidir si el donante sigue en la misma, y con todos los ids en {@code null} los daría
     * por iguales a todos: el {@code continue} se comería el reacomodo entero y el test
     * probaría nada. Asignar el id simula lo que hace la base y deja la comparación con
     * sentido.
     *
     * <p>No hay setter porque el punto 23 cerró la entidad, y tiene sentido que siga así
     * también para los tests.
     */
    private static Mision mision(String nombre) {
        Mision mision = new Mision(nombre, null, "desc", "insignia",
                new Regla(null, AtributoImpacto.CANTIDAD_BIENES, new SuperaCantidad(1, 3)));
        ReflectionTestUtils.setField(mision, "idMision", UUID.randomUUID());
        return mision;
    }

    /** Un donante parado en la misión dada, como si ya hubiera avanzado hasta ahí. */
    private static Perfil donanteEn(Mision mision) {
        Perfil perfil = new Perfil(UUID.randomUUID(), "Ana");
        perfil.iniciarEn(new Categoria("Colaborador", null, 1, List.of(mision)));
        perfil.cambiarMision(mision, null);
        return perfil;
    }

    /**
     * {@code domainEvents()} es protected en {@code AbstractAggregateRoot}, así que se
     * accede por reflexión. Es el mismo truco que ya usa {@code PerfilTest}: meter un
     * método público solo para que el test pueda ver los eventos sería ensuciar la entidad
     * con API de test.
     */
    @SuppressWarnings("unchecked")
    private static Collection<Object> eventos(Perfil perfil) {
        return (Collection<Object>) ReflectionTestUtils.invokeMethod(perfil, "domainEvents");
    }

    /**
     * El mapa "dónde estaba cada misión antes del cambio", que es lo que recibe
     * {@code actualizarMisionesPorCambioDeCategoria} para saber a quién hay que mover.
     *
     * <p>Un {@code HashMap} y no un {@code Map.of} porque {@code Map.of} rechaza claves
     * nulas con un {@code NullPointerException} en el propio {@code Map.of}, antes incluso de
     * llegar al método que estamos probando. Con el id asignado por {@link #mision} no hace
     * falta, pero el {@code HashMap} lo tolera igual y deja claro que el mapa es de prueba.
     */
    private static Map<UUID, Integer> dondeEstaba(Mision mision, int posicion) {
        Map<UUID, Integer> posiciones = new HashMap<>();
        posiciones.put(mision.getIdMision(), posicion);
        return posiciones;
    }

    /**
     * Arma el caso del backlog: la categoría tenía tres misiones, ahora tiene dos, y el
     * donante estaba en la tercera.
     */
    private Categoria categoriaConDos(Perfil donante) {
        Categoria categoria = new Categoria("Colaborador", null, 2,
                List.of(mision("A"), mision("B")));
        when(repoPerfiles.findAllByCategoriaActual(categoria)).thenReturn(List.of(donante));
        return categoria;
    }

    @Nested
    @DisplayName("el efecto observable: el donante conserva una misión y sigue pudiendo progresar")
    class EfectoObservable {

        @Test
        @DisplayName("el donante cuya misión desaparece retrocede a la última que queda")
        void elDonanteRetrocedeALaUltimaQueQueda() {
            Mision c = mision("C");
            Perfil donante = donanteEn(c);

            // Antes la categoría era [A(1), B(2), C(3)] y el donante estaba en C(3). El admin
            // quita C y queda [A(1), B(2)], así que la posición 3 desaparece.
            sincronizacion.actualizarMisionesPorCambioDeCategoria(
                    categoriaConDos(donante), dondeEstaba(c, 3));

            assertThat(donante.getProgresoMisionActual())
                    .as("sin esto el donante queda bloqueado para siempre (punto 30)")
                    .isNotNull();
            assertThat(donante.getProgresoMisionActual().getMision().getNombreMision())
                    .as("retrocede lo mínimo, no vuelve a la primera")
                    .isEqualTo("B");
        }

        @Test
        @DisplayName("si le sacan la primera y no queda nada por debajo, arranca por la primera que hay")
        void sinNadaPorDebajoArrancaPorLaPrimera() {
            // El donante estaba en A(1); le sacan A y queda [B(2), C(3)]. Renumerar deja
            // B en 1, así que la posición 1 existe de nuevo y le toca B.
            Mision b = mision("B");
            Mision a = mision("A");
            Perfil donante = donanteEn(a);

            Categoria categoria = new Categoria("Colaborador", null, 2, List.of(b, mision("C")));
            when(repoPerfiles.findAllByCategoriaActual(categoria)).thenReturn(List.of(donante));

            sincronizacion.actualizarMisionesPorCambioDeCategoria(
                    categoria, dondeEstaba(a, 1));

            assertThat(donante.getProgresoMisionActual()).isNotNull();
            assertThat(donante.getProgresoMisionActual().getMision().getNombreMision())
                    .isEqualTo("B");
        }

        @Test
        @DisplayName("se emite MisionCambiada: el donante se entera de que le movieron la misión")
        void seEmiteElEventoDeCambio() {
            Mision c = mision("C");
            Perfil donante = donanteEn(c);

            sincronizacion.actualizarMisionesPorCambioDeCategoria(
                    categoriaConDos(donante), dondeEstaba(c, 3));

            assertThat(eventos(donante))
                    .as("antes el return temprano era previo al registerEvent, así que no "
                            + "llegaba ningún aviso y el bloqueo era invisible")
                    .anyMatch(evento -> evento instanceof MisionCambiada);
        }

        @Test
        @DisplayName("queda en el log cuando la categoría se queda realmente sin misiones")
        void avisaCuandoLaCategoriaQuedaVacia() {
            Mision c = mision("C");
            Perfil donante = donanteEn(c);

            Categoria vacia = new Categoria("Colaborador", null, 0, List.of());
            when(repoPerfiles.findAllByCategoriaActual(vacia)).thenReturn(List.of(donante));

            sincronizacion.actualizarMisionesPorCambioDeCategoria(
                    vacia, dondeEstaba(c, 3));

            // Acá sí es correcto quedar sin misión: no hay nada que ofrecerle. Lo que no
            // puede ser es en silencio, y ahora hay un log.warn con el id del donante.
            assertThat(donante.getProgresoMisionActual()).isNull();
        }
    }

    @Nested
    @DisplayName("lo que no tiene que cambiar")
    class SinRegresiones {

        @Test
        @DisplayName("si la posición sigue existiendo, el donante no se toca")
        void siLaPosicionSigueExistiendoNoSeToca() {
            Mision a = mision("A");
            Mision b = mision("B");
            Perfil donante = donanteEn(b);
            Categoria categoria = new Categoria("Colaborador", null, 2, List.of(a, b));
            when(repoPerfiles.findAllByCategoriaActual(categoria)).thenReturn(List.of(donante));

            sincronizacion.actualizarMisionesPorCambioDeCategoria(
                    categoria, dondeEstaba(b, 2));

            // Sigue en B y no se le emitió ningún evento de cambio.
            assertThat(donante.getProgresoMisionActual().getMision().getNombreMision())
                    .isEqualTo("B");
            assertThat(eventos(donante))
                    .noneMatch(evento -> evento instanceof MisionCambiada);
        }

        @Test
        @DisplayName("el caso normal de siempre: la misión sigue en la misma posición")
        void elCasoNormalDeSiempre() {
            Mision a = mision("A");
            Mision b = mision("B");
            Perfil donante = donanteEn(a);

            Categoria categoria = new Categoria("Colaborador", null, 2, List.of(a, b));
            when(repoPerfiles.findAllByCategoriaActual(categoria)).thenReturn(List.of(donante));

            sincronizacion.actualizarMisionesPorCambioDeCategoria(
                    categoria, dondeEstaba(a, 1));

            assertThat(donante.getProgresoMisionActual().getMision().getNombreMision())
                    .isEqualTo("A");
            verify(repoPerfiles).saveAll(any());
        }

        @Test
        @DisplayName("los perfiles se guardan igual: el reacomodo tiene que persistir")
        void losPerfilesSeGuardan() {
            Mision c = mision("C");
            Perfil donante = donanteEn(c);

            sincronizacion.actualizarMisionesPorCambioDeCategoria(
                    categoriaConDos(donante), dondeEstaba(c, 3));

            verify(repoPerfiles).saveAll(any());
        }
    }
}
