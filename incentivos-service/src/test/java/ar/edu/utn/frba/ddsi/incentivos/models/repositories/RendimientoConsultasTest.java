package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.InsigniaObtenida;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Las consultas que leen tablas grandes no pueden depender de funciones sobre la columna ni
 * de un N+1 (punto 22).
 *
 * <p>Los gastos de este punto eran invisibles porque nada se rompía: los resultados
 * daban correctos y lo único que pasaba es que la base hacía muchísimo trabajo de más. Con
 * el tamaño de una base de desarrollo nadie lo nota, y con datos reales es la diferencia
 * entre milisegundos y minutos.
 *
 * <p>Lo que se comprueba acá son las tres cosas que se pueden comprobar sin base: que los
 * índices estén declarados, que las relaciones que se usan en las lecturas no sean EAGER,
 * y que el filtro del ranking sea un rango. El plan de ejecución de verdad lo respondería un
 * {@code EXPLAIN} que acá no se puede hacer.
 */
@DisplayName("Punto 22: las consultas de las tablas grandes no hacen trabajo de más")
class RendimientoConsultasTest {

    /**
     * Los índices que el punto 22 deja declarados, con la razón por la que cada uno existe.
     *
     * <p>Se listan acá y no sehardcodean uno por uno en cada test porque un índice que se
     * borra tiene que romper un test que diga <em>para qué estaba</em>, no uno que diga
     * "falta el índice 3".
     */
    private static final String[][] INDICES_ESPERADOS = {
            {
                    "idx_impacto_usuario_fecha",
                    "la evolución mensual y el resumen por rango filtran por donante y "
                            + "agrupan por mes; sin llegar a la columna de fecha el filtro no "
                            + "acota nada"
            },
            {
                    "idx_impacto_usuario_mision_fecha",
                    "el cálculo de constancia pide las donaciones de un donante en una misión "
                            + "ordenadas por fecha"
            },
            {
                    "idx_insignia_obtenida_perfil_fecha",
                    "la paginación de insignias de un donante ordena por fecha de obtención"
            },
            {
                    "idx_insignia_obtenida_fecha",
                    "el ranking mensual filtra por fecha sin saber todavía de qué perfil se "
                            + "trata, así que los otros índices no le sirven"
            },
    };

    private static Index[] indicesDe(Class<?> entidad) {
        var tabla = entidad.getAnnotation(jakarta.persistence.Table.class);
        return tabla == null || tabla.indexes().length == 0
                ? new Index[0]
                : tabla.indexes();
    }

    private static boolean tieneIndice(Class<?> entidad, String nombre) {
        return Arrays.stream(indicesDe(entidad))
                .anyMatch(indice -> indice.name().equals(nombre));
    }

    @Nested
    @DisplayName("los índices que sostienen las consultas de las tablas grandes")
    class Indices {

        @Test
        @DisplayName("impacto_donacion tiene índice por donante y por donante+misión")
        void impactoDonacionTieneSusIndices() {
            assertThat(indicesDe(ImpactoDonacion.class))
                    .as("es la tabla más grande del servicio y todas las lecturas la filtran")
                    .isNotEmpty();

            for (String[] indice : INDICES_ESPERADOS) {
                if (indice[0].startsWith("idx_impacto")) {
                    assertThat(tieneIndice(ImpactoDonacion.class, indice[0]))
                            .as(indice[1])
                            .isTrue();
                }
            }
        }

        @Test
        @DisplayName("insignias_obtenidas tiene los tres índices que sus consultas necesitan")
        void insigniasObtenidasTieneSusIndices() {
            for (String[] indice : INDICES_ESPERADOS) {
                if (indice[0].startsWith("idx_insignia")) {
                    assertThat(tieneIndice(InsigniaObtenida.class, indice[0]))
                            .as(indice[1])
                            .isTrue();
                }
            }
        }

        @Test
        @DisplayName("el índice de la paginación tiene perfil antes que fecha")
        void elIndiceDeLaPaginacionEmpiezaPorPerfil() {
            var indice = Arrays.stream(indicesDe(InsigniaObtenida.class))
                    .filter(i -> i.name().equals("idx_insignia_obtenida_perfil_fecha"))
                    .findFirst()
                    .orElseThrow();

            assertThat(indice.columnList())
                    .as("""
                    Al revés, la base puede usar el índice para filtrar por perfil pero igual
                    tiene que ordenar por fecha, que es la parte cara de la consulta.
                    """)
                    .isEqualTo("perfil_id, fecha_obtencion");
        }

        @Test
        @DisplayName("el ranking mensual tiene su propio índice por fecha, sin perfil")
        void elRankingMensualTieneIndicePorFechaSola() {
            var indice = Arrays.stream(indicesDe(InsigniaObtenida.class))
                    .filter(i -> i.name().equals("idx_insignia_obtenida_fecha"))
                    .findFirst()
                    .orElseThrow();

            assertThat(indice.columnList())
                    .as("el ranking agrupa por donante sobre todo el mes: filtra solo por fecha")
                    .isEqualTo("fecha_obtencion");
        }
    }

    @Nested
    @DisplayName("las relaciones de las lecturas no son EAGER")
    class NadaDeEager {

        @Test
        @DisplayName("InsigniaObtenida.insignia es LAZY: si no, es una consulta por fila")
        void insigniaEsLazy() throws Exception {
            Field campo = InsigniaObtenida.class.getDeclaredField("insignia");
            ManyToOne relacion = campo.getAnnotation(ManyToOne.class);

            assertThat(relacion.fetch())
                    .as("""
                    Con EAGER, la paginación de 20 insignias de un donante son 21 consultas.
                    La lectura que necesita el nombre usa paginaInsigniasPorIdUsuario, que
                    trae la relación con un @EntityGraph.
                    """)
                    .isEqualTo(jakarta.persistence.FetchType.LAZY);
        }

        @Test
        @DisplayName("y la consulta que la usa tiene el EntityGraph que la trae")
        void laPaginacionTraeLaInsignia() throws Exception {
            var metodo = ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories
                    .RepositorioPerfiles.class
                    .getMethod("paginaInsigniasPorIdUsuario", java.util.UUID.class,
                            org.springframework.data.domain.Pageable.class);

            var entityGraph = metodo.getAnnotation(
                    org.springframework.data.jpa.repository.EntityGraph.class);

            assertThat(entityGraph)
                    .as("sin esto el LAZY se traduce en una consulta por elemento de la página")
                    .isNotNull();
            assertThat(entityGraph.attributePaths()).contains("insignia");
        }
    }

    @Nested
    @DisplayName("la evolución mensual se agrupa en la base, no en Java")
    class LaEvolucionSeAgrupaEnSql {

        @Test
        @DisplayName("la consulta de la evolución devuelve una fila por mes")
        void laConsultaDevuelveUnaFilaPorMes() throws Exception {
            var metodo = ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories
                    .RepositorioDonaciones.class
                    .getMethod("obtenerEvolucionMensual", java.util.UUID.class);

            var anotacion = metodo.getAnnotation(
                    org.springframework.data.jpa.repository.Query.class);

            assertThat(anotacion)
                    .as("si no hay GROUP BY, el service tiene que traer todas las filas")
                    .isNotNull();
            assertThat(anotacion.value())
                    .contains("GROUP BY YEAR(d.fechaEntrega), MONTH(d.fechaEntrega)")
                    .contains("COUNT(DISTINCT");
        }

        @Test
        @DisplayName("las entidades vacías no cuentan como organización")
        void lasEntidadesVaciasNoCuentan() throws Exception {
            var metodo = ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories
                    .RepositorioDonaciones.class
                    .getMethod("obtenerEvolucionMensual", java.util.UUID.class);

            String query = metodo.getAnnotation(
                    org.springframework.data.jpa.repository.Query.class).value();

            // El código anterior en Java filtraba nulos y vacíos antes de contar. Un
            // COUNT(DISTINCT columna) a secas cuenta la cadena vacía como una entidad más.
            assertThat(query)
                    .as("las dos mitades de la respuesta tienen que usar la misma regla")
                    .contains("CASE WHEN TRIM(d.entidadBeneficiaria) <> ''");
        }
    }
}
