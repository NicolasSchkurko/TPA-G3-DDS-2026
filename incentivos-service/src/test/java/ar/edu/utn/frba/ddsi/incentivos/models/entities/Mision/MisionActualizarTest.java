package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Mision.actualizar")
class MisionActualizarTest {

    /** La misión con su nombre, su texto y los datos propios de la insignia. */
    private Mision mision(String nombre, String descripcion,
                         String insigniaNombre, String insigniaDescripcion,
                         Integer objetivo) {
        return new Mision(
                nombre,
                null,
                descripcion,
                insigniaNombre,
                insigniaDescripcion,
                null,
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
        @DisplayName("cambiar el texto de la mision no toca el texto de la insignia")
        void cambiarElTextoDeLaMisionNoPisaLaInsignia() {
            // La insignia tiene su propio texto, así que editar el de la misión no lo mueve.
            // Antes le llegaba this.descripcion y quedaba pegado al texto de la misión.
            Mision actual = mision("Diez dones", "Texto viejo", "Constante",
                    "Texto propio de la insignia", 10);
            assertThat(actual.getInsigniaObjetivo().getDescripcion())
                    .isEqualTo("Texto propio de la insignia");

            Mision editada = mision("Diez dones", "TEXTO NUEVO de la mision", "Constante",
                    "Texto propio de la insignia", 10);
            actual.actualizar(editada);

            assertThat(actual.getDescripcion()).isEqualTo("TEXTO NUEVO de la mision");
            assertThat(actual.getInsigniaObjetivo().getDescripcion())
                    .as("el texto de la insignia y el de la misión son campos separados")
                    .isEqualTo("Texto propio de la insignia");
        }

        @Test
        @DisplayName("la insignia toma su texto de la insignia entrante, no del nombre de la misión")
        void laInsigniaTomaSuDescripcionDeLaInsigniaEntrante() {
            Mision actual = mision("Diez dones", "Texto viejo", "Constante",
                    "Texto viejo de la insignia", 10);

            Mision editada = mision("Quince dones", "Otro texto de mision", "Constante",
                    "Texto nuevo de la insignia", 10);
            actual.actualizar(editada);

            // Antes el nombre de la misión era lo que quedaba en la insignia, porque el
            // constructor no recibía otro texto y lo derivaba de ahí (punto 24).
            assertThat(actual.getInsigniaObjetivo().getDescripcion())
                    .isEqualTo("Texto nuevo de la insignia");
            assertThat(actual.getDescripcion()).isEqualTo("Otro texto de mision");
        }

        @Test
        @DisplayName("editar solo la imagen no exige mandar el nombre")
        void editarSoloLaImagenNoExigeElNombre() {
            // La condición del punto 15 era `insigniaNueva.getNombre() != null`, así que un
            // PUT que solo cambiaba la imagen no se aplicaba. Con los tres campos
            // independientes, cada uno se aplica solo si viene.
            Mision actual = new Mision("Diez dones", null, "Texto", "La insignia",
                    "Texto de la insignia", null,
                    new Regla(null, AtributoImpacto.ESTADO, new SuperaCantidad(10, 1)));

            Mision editada = new Mision(null, null, null, null,
                    null, "https://incentivos.example.edu.ar/img/nueva.png",
                    new Regla(null, AtributoImpacto.ESTADO, new SuperaCantidad(10, 1)));
            actual.actualizar(editada);

            assertThat(actual.getInsigniaObjetivo().getUrlImagen())
                    .isEqualTo("https://incentivos.example.edu.ar/img/nueva.png");
            assertThat(actual.getInsigniaObjetivo().getNombre()).isEqualTo("La insignia");
            assertThat(actual.getInsigniaObjetivo().getDescripcion())
                    .isEqualTo("Texto de la insignia");
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
