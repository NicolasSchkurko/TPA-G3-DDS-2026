package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.MisionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La insignia tiene sus propios tres datos, y el umbral de "supera" es exclusivo.
 *
 * <p>Son dos cosas que parecen de texto pero no lo son. La del punto 24 es de modelo: el
 * enunciado pide que la insignia tenga nombre, descripción e imagen, y solo se podía cargar
 * uno de los tres porque los otros dos no tenían por dónde entrar.
 */
@DisplayName("Puntos 33 y 24: la insignia tiene sus datos y el umbral es exclusivo")
class InsigniaYOperacionSemanticaTest {

    private static Mision misionCon(Insignia insignia) {
        return new Mision(
                "Diez dones",
                null,
                "Texto de la MISION",
                insignia.getNombre(),
                insignia.getDescripcion(),
                insignia.getUrlImagen(),
                new Regla(null, AtributoImpacto.ESTADO, new SuperaCantidad(10, 1))
        );
    }

    @Nested
    @DisplayName("Punto 24: la insignia tiene sus propios datos")
    class LaInsigniaTieneSusDatos {

        @Test
        @DisplayName("el texto de la insignia NO es el nombre de la misión")
        void elTextoNoEsElNombreDeLaMision() {
            Insignia insignia = new Insignia("Constante", "Texto PROPIO de la insignia", null);

            Mision mision = misionCon(insignia);

            // Antes el constructor hacía new Insignia(nombreInsignia, nombre), o sea que el
            // texto de la insignia era el nombre de la misión. "Constante" de texto para una
            // insignia que se llama "Constante" no es lo mismo que "Texto PROPIO de la
            // insignia", y el enunciado pide lo segundo.
            assertThat(mision.getInsigniaObjetivo().getDescripcion())
                    .isEqualTo("Texto PROPIO de la insignia");

            assertThat(mision.getDescripcion())
                    .as("el de la misión sigue siendo el de la misión")
                    .isEqualTo("Texto de la MISION");
        }

        @Test
        @DisplayName("sin texto propio la descripción queda en null, no inventada")
        void sinTextoPropioQuedaNull() {
            // El atajo de 5 parámetros deja la insignia sin descripción. Antes eso era
            // imposible: el constructor rellenaba el campo con el nombre de la misión. Un
            // null dice "no hay texto"; el nombre de la misión decía algo falso.
            Mision mision = new Mision(
                    "Diez dones", null, "Texto", "Constante",
                    new Regla(null, AtributoImpacto.ESTADO, new SuperaCantidad(10, 1))
            );

            assertThat(mision.getInsigniaObjetivo().getDescripcion()).isNull();
        }

        @Test
        @DisplayName("la imagen se puede cargar: antes urlImagen quedaba siempre en null")
        void laImagenSePuedeCargar() throws Exception {
            Mision mision = misionCon(new Insignia(
                    "Constante", "Texto propio", "https://incentivos.example.edu.ar/img/constancia.png"));

            assertThat(mision.getInsigniaObjetivo().getUrlImagen())
                    .isEqualTo("https://incentivos.example.edu.ar/img/constancia.png");

            // Y la columna existía: no era que faltara el dato, era que no había por dónde
            // cargarlo.
            Field imagen = Insignia.class.getDeclaredField("urlImagen");
            assertThat(imagen.getType()).isEqualTo(String.class);
        }

        @Test
        @DisplayName("el DTO expone los tres datos de la insignia")
        void elDtoExponeLosTresDatos() {
            // El enunciado pide nombre, descripción e imagen. Antes solo el nombre existía en
            // el DTO, así que los otros dos no se podían mandar ni leer.
            MisionDTO proyectado = MisionDTO.desdeEntidad(misionCon(new Insignia(
                    "Constancia solidaria",
                    "Una donación por mes durante tres meses.",
                    "https://incentivos.example.edu.ar/img/constancia.png")));

            assertThat(proyectado.getInsigniaObjetivo()).isEqualTo("Constancia solidaria");
            assertThat(proyectado.getInsigniaDescripcion())
                    .isEqualTo("Una donación por mes durante tres meses.");
            assertThat(proyectado.getInsigniaUrlImagen())
                    .isEqualTo("https://incentivos.example.edu.ar/img/constancia.png");
        }

        @Test
        @DisplayName("los campos de la insignia son opcionales: no rompen los clientes de hoy")
        void losCamposSonOpcionales() throws Exception {
            // Obligar a los tres sería romper cualquier cliente que mande solo el nombre,
            // que es lo que todos los de hoy hacen. La insignia se puede cargar entera, pero
            // no tiene que estarlo.
            assertThat(MisionDTO.class.getDeclaredField("insigniaDescripcion")
                    .getAnnotation(jakarta.validation.constraints.NotBlank.class))
                    .isNull();
            assertThat(MisionDTO.class.getDeclaredField("insigniaUrlImagen")
                    .getAnnotation(jakarta.validation.constraints.NotBlank.class))
                    .isNull();
            assertThat(MisionDTO.class.getDeclaredField("insigniaObjetivo")
                    .getAnnotation(jakarta.validation.constraints.NotBlank.class))
                    .as("el nombre sí sigue siendo obligatorio")
                    .isNotNull();
        }
    }

    @Nested
    @DisplayName("Punto 33: 'supera' es estricto")
    class SuperaCantidadEsEstricta {

        @Test
        @DisplayName("el valor exacto NO cuenta: hay que superarlo")
        void elValorExactoNoCuenta() {
            SuperaCantidad operacion = new SuperaCantidad(1, 6);

            assertThat(operacion.calcularProgreso(6, null))
                    .as("la misión del seed dice 'supera 6 bienes', y superarla es 7 o más")
                    .isFalse();
        }

        @Test
        @DisplayName("un bien más sí cuenta")
        void unBienMasSiCuenta() {
            assertThat(new SuperaCantidad(1, 6).calcularProgreso(7, null)).isTrue();
            assertThat(new SuperaCantidad(1, 6).calcularProgreso(100, null)).isTrue();
        }

        @Test
        @DisplayName("el nombre de la operación y su semántica dicen lo mismo")
        void elNombreYLaSemanticaDicenLoMismo() {
            // La operación se llama SuperaCantidad. Con un >=, el nombre mentía: "supera 6"
            // contando desde 6. Los tres —nombre del enunciado, nombre de la clase,
            // comparación— tienen que coincidir, o el que miente es el código.
            assertThat(SuperaCantidad.class.getSimpleName()).isEqualTo("SuperaCantidad");

            // Y el umbral es exclusivo: con 6, el primer valor que cuenta es 7.
            assertThat(new SuperaCantidad(1, 6).calcularProgreso(7, null)).isTrue();
            assertThat(new SuperaCantidad(1, 6).calcularProgreso(6, null)).isFalse();
        }

        @Test
        @DisplayName("subir el umbral no invalida lo ya acreditado")
        void subirElUmbralNoInvalidaLoAcreditado() {
            // Esta parte no cambió con el punto 33 y conviene tenerla presente: si el umbral
            // se comparara en esEquivalenteA, retocarlo le borraría el progreso a todos los
            // que estaban en la misión.
            assertThat(new SuperaCantidad(5, 4).esEquivalenteA(new SuperaCantidad(5, 6)))
                    .isTrue();
        }
    }
}
