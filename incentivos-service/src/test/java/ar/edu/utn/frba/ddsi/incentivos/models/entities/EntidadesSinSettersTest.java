package ar.edu.utn.frba.ddsi.incentivos.models.entities;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.CategoriaMision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Higiene del agregado: las entidades no tienen setters")
class EntidadesSinSettersTest {

    /** Las entidades contra las que se corre la red de seguridad. */
    private static final List<Class<?>> ENTIDADES = List.of(
            Perfil.class,
            ProgresoMision.class,
            ImpactoDonacion.class,
            Categoria.class,
            CategoriaMision.class,
            Mision.class,
            Insignia.class,
            Regla.class,
            ReglaConstancia.class,
            Operacion.class,
            Ranking.class,
            RankingMensual.class,
            MedioContacto.class
    );

    /** Las que el test de setters mira: sin Operacion (abstracta) ni ImpactoDonacion (su id). */
    static List<Class<?>> entidadesSinSetters() {
        return ENTIDADES.stream()
                .filter(clase -> !clase.equals(Operacion.class))
                .filter(clase -> !clase.equals(ImpactoDonacion.class))
                .toList();
    }

    @ParameterizedTest
    @MethodSource("entidadesSinSetters")
    @DisplayName("ninguna entidad expone un setter público")
    void ningunaEntidadExponeSetters(Class<?> entidad) {
        List<String> setters = Arrays.stream(entidad.getMethods())
                .filter(m -> m.getName().startsWith("set"))
                .map(m -> m.getDeclaringClass().getSimpleName() + "." + m.getName())
                .toList();

        assertThat(setters)
                .as("%s no debería tener setters públicos", entidad.getSimpleName())
                .isEmpty();
    }

    @Test
    @DisplayName("las operaciones no tienen setters tampoco")
    void lasOperacionesNoTienenSetters() {
        // Se prueban por separado porque son abstractas: getMethods() sobre la clase base
        // no muestra lo que definen las subclases.
        List<Class<?>> operaciones = List.of(
                ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones
                        .SuperaCantidad.class,
                ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones
                        .CantidadCoincidencias.class,
                ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones
                        .ValoresDistintos.class
        );

        for (Class<?> operacion : operaciones) {
            assertThat(Arrays.stream(operacion.getMethods())
                    .filter(m -> m.getName().startsWith("set"))
                    .toList())
                    .as("%s no debería tener setters", operacion.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("el id de la donación sí se puede asignar: lo pone el servicio de origen")
    void elIdDeLaDonacionSePuedeAsignar() {
        // Única excepción: idDonacion lo asigna el servicio de origen y hace idempotente la ingesta.
        assertThat(tieneSetter(ImpactoDonacion.class, "idDonacion")).isTrue();
        assertThat(tieneSetter(ImpactoDonacion.class, "completMision")).isFalse();
        assertThat(tieneSetter(ImpactoDonacion.class, "hizoProgresarMision")).isFalse();
        assertThat(tieneSetter(ImpactoDonacion.class, "idMision")).isFalse();
    }

    @Test
    @DisplayName("los métodos de negocio del perfil no son estáticos ni privados")
    void losMetodosDeNegocioSonUsables() {
        // Si alguno se hubiera hecho private por error, el service no compilaría; el punto
        // es dejar constancia de que la encapsulación es solo de escritura.
        for (String metodo : List.of("iniciarEn", "finalizarSecuencia", "cambiarNombre",
                "cambiarMision", "cambiarCategoria", "progresarMision")) {
            Method m = buscarMetodo(Perfil.class, metodo);
            assertThat(m).as("Perfil.%s debería existir", metodo).isNotNull();
            assertThat(Modifier.isStatic(m.getModifiers()))
                    .as("Perfil.%s no debería ser static", metodo).isFalse();
            assertThat(Modifier.isPrivate(m.getModifiers()))
                    .as("Perfil.%s no debería ser private", metodo).isFalse();
        }
    }

    private static Method buscarMetodo(Class<?> tipo, String nombre) {
        return Arrays.stream(tipo.getDeclaredMethods())
                .filter(m -> m.getName().equals(nombre))
                .findFirst()
                .orElse(null);
    }

    private static boolean tieneSetter(Class<?> tipo, String campo) {
        String setter = "set" + campo.substring(0, 1).toUpperCase() + campo.substring(1);
        return Arrays.stream(tipo.getMethods()).anyMatch(m -> m.getName().equals(setter));
    }
}
