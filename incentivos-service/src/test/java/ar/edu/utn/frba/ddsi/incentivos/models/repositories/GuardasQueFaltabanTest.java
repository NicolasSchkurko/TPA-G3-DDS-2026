package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.UnidadTiempo;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.OperacionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioMisiones;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@DisplayName("Punto 34: guardas que el codigo decia tener y no tenia")
class GuardasQueFaltabanTest {

    @Nested
    @DisplayName("a) el enum se parseaba sin normalizar en el filtro del listado")
    class ElEnumDelListado {

        /** Un repositorio con el filtro de {@code obtenerTodas} de verdad (CALLS_REAL_METHODS). */
        private RepositorioMisiones repositorioConFiltroReal() {
            RepositorioMisiones repo = mock(RepositorioMisiones.class,
                    withSettings().defaultAnswer(CALLS_REAL_METHODS));

            when(repo.findAllByFiltros(any(), any(), any(), any()))
                    .thenAnswer(invoc -> new PageImpl<Mision>(java.util.List.of()));

            return repo;
        }

        @Test
        @DisplayName("'CATEGORÍA' con acento se acepta, como en el POST")
        void conAcentoSeAcepta() {
            RepositorioMisiones repo = repositorioConFiltroReal();

            assertThat(repo.obtenerTodas(null, null, "CATEGORÍA", Pageable.unpaged()))
                    .as("""
                    El POST de la misma misión acepta "CATEGORÍA": MisionFactory.crearAtributoImpacto
                    normaliza y saca los acentos. El GET con query param no lo hacía y devolvía
                    400 con el mensaje crudo de IllegalArgumentException. Era la misma entrada
                    por dos caminos y solo uno entendía español.
                    """)
                    .isNotNull();
        }

        @Test
        @DisplayName("'categoria' en minúsculas y con espacios también se acepta")
        void conMinusculasYEspacios() {
            assertThat(repositorioConFiltroReal()
                    .obtenerTodas(null, null, "  categoria  ", Pageable.unpaged()))
                    .isNotNull();
        }

        @Test
        @DisplayName("el enum que llega a la consulta es el del enum, no la cadena cruda")
        void elEnumQueLlegaEsElDelEnum() {
            RepositorioMisiones repo = repositorioConFiltroReal();

            when(repo.findAllByFiltros(any(), any(), any(), any()))
                    .thenAnswer(invocacion -> {
                        AtributoImpacto recibido = invocacion.getArgument(2);
                        assertThat(recibido).isEqualTo(AtributoImpacto.CATEGORIA);
                        return new PageImpl<Mision>(java.util.List.of());
                    });

            repo.obtenerTodas(null, null, "CATEGORÍA", Pageable.unpaged());
        }

        @Test
        @DisplayName("un valor inválido da 400 con la lista de los válidos, no un 500")
        void unValorInvalidoDa400Util() {
            RepositorioMisiones repo = repositorioConFiltroReal();

            assertThatThrownBy(() -> repo.obtenerTodas(null, null, "INVENTADO", Pageable.unpaged()))
                    .isInstanceOf(DatosInvalidosException.class)
                    .hasMessageContaining("CATEGORIA")
                    .hasMessageContaining("ESTADO");

            // Antes salía un IllegalArgumentException crudo, atado al GlobalExceptionHandler.
        }

        @Test
        @DisplayName("vacío o ausente no filtra, no explota")
        void vacioNoFiltra() {
            RepositorioMisiones repo = repositorioConFiltroReal();

            assertThat(repo.obtenerTodas(null, null, null, Pageable.unpaged())).isNotNull();
            assertThat(repo.obtenerTodas(null, null, "   ", Pageable.unpaged())).isNotNull();
        }
    }

    @Nested
    @DisplayName("b) crearConstancia decia rechazar y no rechazaba")
    class CrearConstancia {

        private final MisionFactory factory = new MisionFactory(new OperacionFactory());

        @Test
        @DisplayName("ninguna parte presente es válido: la misión no exige ventana temporal")
        void ningunaParteEsValido() {
            assertThat(factory.crearConstancia(null, null)).isNull();
        }

        @Test
        @DisplayName("solo la unidad se rechaza: '3' sin cantidad no significa nada")
        void soloLaUnidadSeRechaza() {
            assertThatThrownBy(() -> factory.crearConstancia(null, "MESES"))
                    .isInstanceOf(DatosInvalidosException.class);
        }

        @Test
        @DisplayName("solo la cantidad se rechaza")
        void soloLaCantidadSeRechaza() {
            assertThatThrownBy(() -> factory.crearConstancia(3, null))
                    .isInstanceOf(DatosInvalidosException.class);
        }

        @Test
        @DisplayName("las dos partes dan una constancia normal")
        void lasDosPartesDanConstancia() {
            ReglaConstancia constancia = factory.crearConstancia(3, "MESES");

            assertThat(constancia.getCantidad()).isEqualTo(3);
            assertThat(constancia.getUnidadTiempo()).isEqualTo(UnidadTiempo.MONTHS);
        }
    }
}
