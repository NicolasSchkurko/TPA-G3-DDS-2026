package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.CategoriaBaseInexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.PerfilExistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.ValidadorAdmin;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

@DisplayName("Punto 25: crearPerfil arma al donante con su primera misión")
class PerfilServiceAltaTest {

    private final RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
    private final RepositorioCategorias repoCategorias = mock(RepositorioCategorias.class);
    private final PerfilService service = new PerfilService(
            repoPerfiles,
            repoCategorias,
            mock(RepositorioDonaciones.class),
            mock(TransactionTemplate.class),
            mock(ValidadorAdmin.class)
    );

    private static Categoria categoriaCon(String... nombresMision) {
        List<Mision> misiones = new java.util.ArrayList<>();
        for (String nombre : nombresMision) {
            misiones.add(new Mision(nombre, null, "d", "i",
                    new Regla(null, AtributoImpacto.CANTIDAD_BIENES, new SuperaCantidad(1, 1))));
        }
        return new Categoria("Colaborador", null, 1, misiones);
    }

    private static PerfilDonanteDTO dto() {
        PerfilDonanteDTO dto = new PerfilDonanteDTO();
        dto.setIdUsuario(UUID.randomUUID());
        dto.setNombreUsuario("Ana");
        return dto;
    }

    /** El repositorio devuelve lo que le pasan, para poder inspeccionar lo guardado. */
    private Perfil guardarYDevolver() {
        when(repoPerfiles.save(any(Perfil.class))).thenAnswer(invoc -> invoc.getArgument(0));
        return null;
    }

    @Nested
    @DisplayName("el efecto observable: el donante sale con misión")
    class EfectoObservable {

        @Test
        @DisplayName("el perfil guardado arranca en la primera misión de la categoría base")
        void elPerfilArrancaEnLaPrimeraMision() {
            Categoria base = categoriaCon("Primera", "Segunda");
            when(repoCategorias.obtenerCategoriaBase()).thenReturn(Optional.of(base));
            guardarYDevolver();

            var resultado = service.crearPerfil(dto());

            assertThat(resultado.getCategoriaActual()).isEqualTo("Colaborador");
            assertThat(resultado.getMisionActual())
                    .as("un donante sin misión no progresa nunca: es el bug del punto 25")
                    .isNotNull();
            assertThat(resultado.getMisionActual()).isEqualTo("Primera");
        }

        @Test
        @DisplayName("la categoría base se pide con su secuencia de misiones, no suelta")
        void laCategoriaBaseSeTraeConSusMisiones() {
            when(repoCategorias.obtenerCategoriaBase())
                    .thenReturn(Optional.of(categoriaCon("Primera")));
            guardarYDevolver();

            service.crearPerfil(dto());

            // No se usa findAllByOrderByPosicionSecuenciaAsc() + findFirst(): esa consulta
            // devuelve la categoría desligada y su colección LAZY no se puede tocar.
            verify(repoCategorias).obtenerCategoriaBase();
        }

        @Test
        @DisplayName("una categoría base sin misiones deja al donante sin misión, no con una en null")
        void unaCategoriaSinMisionesDejaAlDonanteSinMision() {
            when(repoCategorias.obtenerCategoriaBase()).thenReturn(Optional.of(categoriaCon()));
            guardarYDevolver();

            var resultado = service.crearPerfil(dto());

            assertThat(resultado.getCategoriaActual()).isEqualTo("Colaborador");
            assertThat(resultado.getMisionActual()).isNull();
        }

        @Test
        @DisplayName("sin categoría base configurada es un error, no un donante a medias")
        void sinCategoriaBaseDaError() {
            when(repoCategorias.obtenerCategoriaBase()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.crearPerfil(dto()))
                    .isInstanceOf(CategoriaBaseInexistenteException.class);
        }

        @Test
        @DisplayName("un donante que ya tiene perfil no se duplica")
        void unDonanteConPerfilNoSeDuplica() {
            when(repoPerfiles.existsById(any())).thenReturn(true);

            assertThatThrownBy(() -> service.crearPerfil(dto()))
                    .isInstanceOf(PerfilExistenteException.class);

            // Y no se toca la base: si ya existe, no hay nada que resolver.
            verify(repoCategorias, org.mockito.Mockito.never()).obtenerCategoriaBase();
        }
    }

    @Nested
    @DisplayName("el contrato que evita la regresión")
    class Contrato {

        @Test
        @DisplayName("crearPerfil abre transacción: sin ella la colección LAZY está desligada")
        void crearPerfilAbreTransaccion() throws Exception {
            Method metodo = PerfilService.class.getMethod("crearPerfil", PerfilDonanteDTO.class);

            assertThat(metodo.getAnnotation(Transactional.class))
                    .as("""
                    Sin @Transactional, la consulta de la categoría base corre en su propia
                    transacción read-only y devuelve el objeto desligado. Después
                    primeraMision() toca una colección LAZY sin sesión y o bien lanza
                    LazyInitializationException (500 en el alta), o bien el isEmpty() del
                    PersistentBag devuelve el tamaño cacheado, devuelve true en silencio y el
                    donante queda sin misión PARA SIEMPRE.
                    """)
                    .isNotNull();
        }

        @Test
        @DisplayName("la consulta de la categoría base trae las misiones en la misma ida")
        void laConsultaTraeLasMisiones() throws Exception {
            Method metodo = RepositorioCategorias.class.getMethod("obtenerCategoriaBase");

            var query = metodo.getAnnotation(org.springframework.data.jpa.repository.Query.class);

            assertThat(query).as("sin el fetch la categoría vuelve con la colección sin cargar")
                    .isNotNull();
            assertThat(query.value().toUpperCase())
                    .as("el fetch join es lo que evita depender del alcance de la transacción")
                    .contains("FETCH");
        }

        @Test
        @DisplayName("iniciarEn es quien deja la categoría y la misión, no el service")
        void iniciarEnArmaElEstadoInicial() throws Exception {
            // El punto 23 sacó los setters: iniciarEn es el que arma el estado inicial.
            Method metodo = Perfil.class.getMethod("iniciarEn", Categoria.class);

            assertThat(metodo).isNotNull();
            Perfil perfil = new Perfil(UUID.randomUUID(), "Ana");
            perfil.iniciarEn(categoriaCon("Primera"));

            assertThat(ReflectionTestUtils.getField(perfil, "progresoMisionActual"))
                    .as("iniciarEn tiene que dejar la primera misión, no null")
                    .isNotNull();
        }
    }
}
