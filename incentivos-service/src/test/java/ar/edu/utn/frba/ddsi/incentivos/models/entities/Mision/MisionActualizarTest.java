package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Editar una misión (punto 15).
 */
@DisplayName("Mision.actualizar")
class MisionActualizarTest {

    private Mision mision(String nombre, String descripcion,
                         String insigniaNombre, String insigniaDescripcion,
                         Integer objetivo) {
        return new Mision(
                nombre,
                null,
                descripcion,
                insigniaNombre,
                new Regla(null, AtributoImpacto.ESTADO, new SuperaCantidad(objetivo, 1))
        );
    }

    @Nested
    @DisplayName("El progreso de los demás")
    class ProgresoDeLosDemas {

        @Test
        @DisplayName("cambiar solo la descripción NO reinicia el progreso")
        void cambiarLaDescripcionNoReinicia() {
            Mision actual = mision("Diez dones", "Donar diez veces", "Constante", "Insignia", 10);
            Mision editada = mision("Diez dones", "Donar diez veces, sin límite", "Constante", "Insignia", 10);

            // Antes esto devolvía void y el service reiniciaba siempre el progreso.
            assertThat(actual.actualizar(editada)).isFalse();
            assertThat(actual.getDescripcion()).isEqualTo("Donar diez veces, sin límite");
        }

        @Test
        @DisplayName("cambiar el nombre NO reinicia el progreso")
        void cambiarElNombreNoReinicia() {
            Mision actual = mision("Diez dones", "Donar diez veces", "Constante", "Insignia", 10);
            Mision editada = mision("Diez dones cada vez", "Donar diez veces", "Constante", "Insignia", 10);

            assertThat(actual.actualizar(editada)).isFalse();
        }

        @Test
        @DisplayName("cambiar el objetivo de la regla SÍ reinicia el progreso")
        void cambiarElObjetivoSiReinicia() {
            Mision actual = mision("Diez dones", "Donar diez veces", "Constante", "Insignia", 10);
            Mision editada = mision("Diez dones", "Donar diez veces", "Constante", "Insignia", 15);

            assertThat(actual.actualizar(editada)).isTrue();
        }
    }

    @Nested
    @DisplayName("La insignia objetivo")
    class InsigniaObjetivo {

        @Test
        @DisplayName("cambiar el texto de la mision no pisa la descripcion de la insignia")
        void cambiarElTextoDeLaMisionNoPisaLaInsignia() {
            // Al crearse, la insignia deriva su descripcion del NOMBRE de la mision:
            // es lo unico que hay, porque ni el DTO ni el constructor reciben un texto
            // propio para la insignia.
            Mision actual = mision("Diez dones", "Texto viejo", "Constante", "Constante", 10);
            assertThat(actual.getInsigniaObjetivo().getDescripcion()).isEqualTo("Diez dones");

            Mision editada = mision("Diez dones", "TEXTO NUEVO de la mision", "Constante", "Constante", 10);
            actual.actualizar(editada);

            // Antes le llegaba this.descripcion, asi que la insignia quedaba con el texto
            // de la MISION y cambiaba cada vez que se editaba la mision.
            assertThat(actual.getInsigniaObjetivo().getDescripcion()).isEqualTo("Diez dones");
        }

        @Test
        @DisplayName("la insignia toma su descripcion de la insignia entrante, no de la mision")
        void laInsigniaTomaSuDescripcionDeLaInsigniaEntrante() {
            Mision actual = mision("Diez dones", "Texto viejo", "Constante", "Constante", 10);

            // Editar el nombre de la mision cambia la descripcion que el constructor le
            // deriva a la insignia, y eso es lo que debe quedar: no el texto de la mision.
            Mision editada = mision("Quince dones", "Otro texto de mision", "Constante", "Constante", 10);
            actual.actualizar(editada);

            assertThat(actual.getInsigniaObjetivo().getDescripcion()).isEqualTo("Quince dones");
            assertThat(actual.getDescripcion()).isEqualTo("Otro texto de mision");
        }

        @Test
        @DisplayName("se conserva el mismo objeto insignia para no romper las ya obtenidas")
        void seConservaElMismoObjetoInsignia() {
            Mision actual = mision("Diez dones", "Texto", "Constante", "La insignia", 10);
            Object insigniaOriginal = actual.getInsigniaObjetivo();

            Mision editada = mision("Diez dones", "Otro texto", "OTRA insignia", "Otra descripcion", 10);
            actual.actualizar(editada);

            assertThat(actual.getInsigniaObjetivo()).isSameAs(insigniaOriginal);
            assertThat(actual.getInsigniaObjetivo().getNombre()).isEqualTo("OTRA insignia");
        }
    }

    @Nested
    @DisplayName("La regla de progreso")
    class LaRegla {

        @Test
        @DisplayName("si la regla es equivalente se conserva la existente, no se reemplaza")
        void siLaReglaEsEquivalenteSeConservaLaExistente() {
            Mision actual = mision("Diez dones", "Texto", "Constante", "Insignia", 10);
            Object reglaOriginal = actual.getReglaDeProgreso();

            Mision editada = mision("Diez dones", "Otro texto", "Constante", "Insignia", 10);
            actual.actualizar(editada);

            // Guardar una regla nueva dejaría huérfanas la regla y su operación viejas.
            assertThat(actual.getReglaDeProgreso()).isSameAs(reglaOriginal);
        }

        @Test
        @DisplayName("si la regla cambia de verdad se reemplaza")
        void siLaReglaCambiaSeReemplaza() {
            Mision actual = mision("Diez dones", "Texto", "Constante", "Insignia", 10);
            Object reglaOriginal = actual.getReglaDeProgreso();

            Mision editada = mision("Diez dones", "Texto", "Constante", "Insignia", 20);
            actual.actualizar(editada);

            assertThat(actual.getReglaDeProgreso()).isNotSameAs(reglaOriginal);
        }
    }
}
