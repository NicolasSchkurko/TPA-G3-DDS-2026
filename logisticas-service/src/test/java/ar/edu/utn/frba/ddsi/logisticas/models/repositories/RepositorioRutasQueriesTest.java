package ar.edu.utn.frba.ddsi.logisticas.models.repositories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.repository.Query;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RepositorioRutasQueriesTest {

    @Test
    @DisplayName("findFirstByParadasItemsIdDonacion y findFirstByCamionAsignadoChoferIdChoferAndEstado son derived")
    void lasBusquedasSonDerivedYNoEscaneanEnMemoria() {
        List<String> metodos = List.of(
                "findFirstByParadasItemsIdDonacion",
                "findFirstByCamionAsignadoChoferIdChoferAndEstado",
                "findByEstadoIn");

        for (String nombre : metodos) {
            Method metodo = Arrays.stream(RepositorioRutas.class.getDeclaredMethods())
                    .filter(m -> m.getName().equals(nombre))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Falta el método " + nombre));

            assertThat(metodo.isDefault())
                    .as(nombre + " no debe escanear la colección en memoria")
                    .isFalse();
            assertThat(metodo.isAnnotationPresent(Query.class))
                    .as(nombre + " debe ser derived (First) para limitar a un resultado")
                    .isFalse();
        }
    }
}
