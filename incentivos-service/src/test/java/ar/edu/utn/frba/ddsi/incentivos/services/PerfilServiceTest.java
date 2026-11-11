package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.MisionPerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.PerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingFilaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilPublicoDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ResultadoLotePerfilesDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.CategoriaBaseInexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.PerfilExistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.InsigniaObtenida;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioRankings;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@DisplayName("PerfilService")
class PerfilServiceTest {
        @Nested
        @DisplayName("PerfilService: la mision vigente del donante viaja con su avance")
        class MisionVigente {


        private final PerfilService service = new PerfilService(
                mock(RepositorioPerfiles.class),
                mock(RepositorioCategorias.class),
                mock(RepositorioDonaciones.class),
                mock(TransactionTemplate.class)
        );

        /** Un progreso con el avance dado, logrado con donaciones reales. */
        private static ProgresoMision progresoDe(Integer avance, Integer objetivo) {
            Mision mision = new Mision(
                    "Diez dones",
                    null,
                    "Donar diez veces",
                    "Constante",
                    new Regla(null, AtributoImpacto.ESTADO,
                            new CantidadCoincidencias(objetivo, new ObjectMapper().valueToTree("ENTREGADA")))
            );

            ProgresoMision progreso = new ProgresoMision(mision);

            for (int i = 0; i < (avance == null ? 0 : avance); i++) {
                progreso.progresarMision(new ImpactoDonacion(
                        UUID.randomUUID(), UUID.randomUUID(),
                        "Fundacion", 3,
                        LocalDateTime.of(2026, 3, i + 1, 10, 0),
                        "ALIMENTOS", "MERCEARIA", "ENTREGADA"), List.of());
            }

            if (avance == null) {
                // Un progreso recien cargado de la base puede no tener el contador aun.
                ReflectionTestUtils.setField(progreso, "progreso", null);
            }

            return progreso;
        }

        @Test
        @DisplayName("la descripcion y la insignia objetivo ya no salen invertidas")
        void descripcionEInsigniaNoSalenInvertidas() {
            MisionPerfilDTO dto = service.convertirProgresoMisionADTO(progresoDe(2, 10));

            assertThat(dto.getNombreMision()).isEqualTo("Diez dones");
            assertThat(dto.getDescripcion()).isEqualTo("Donar diez veces");
            assertThat(dto.getInsigniaObjetivo()).isEqualTo("Constante");
        }

        @Test
        @DisplayName("expone cuanto lleva, cuanto necesita y cuanto le falta")
        void exponeActualObjetivoYFaltante() {
            MisionPerfilDTO dto = service.convertirProgresoMisionADTO(progresoDe(4, 10));

            assertThat(dto.getProgresoActual()).isEqualTo(4);
            assertThat(dto.getProgresoObjetivo()).isEqualTo(10);
            assertThat(dto.getProgresoFaltante()).isEqualTo(6);
        }

        @Test
        @DisplayName("una mision ya cumplida no muestra progreso faltante negativo")
        void unaMisionCumplidaNoTieneFaltanteNegativo() {
            MisionPerfilDTO dto = service.convertirProgresoMisionADTO(progresoDe(12, 10));

            assertThat(dto.getProgresoFaltante()).isZero();
        }

        @Test
        @DisplayName("un progreso sin valor cargado se muestra como cero, no como null")
        void unProgresoSinValorSeMuestraComoCero() {
            MisionPerfilDTO dto = service.convertirProgresoMisionADTO(progresoDe(null, 10));

            assertThat(dto.getProgresoActual()).isZero();
            assertThat(dto.getProgresoFaltante()).isEqualTo(10);
        }
        }

        @Nested
        @DisplayName("Punto 25: crearPerfil arma al donante con su primera misión")
        class Alta {


        private final RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
        private final RepositorioCategorias repoCategorias = mock(RepositorioCategorias.class);
        private final PerfilService service = new PerfilService(
                repoPerfiles,
                repoCategorias,
                mock(RepositorioDonaciones.class),
                mock(TransactionTemplate.class)
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

        @Nested
        @DisplayName("Punto 36: @Version y reintento ante carrera de concurrencia")
        class Concurrencia {


        private final RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
        private final RepositorioDonaciones repoDonaciones = mock(RepositorioDonaciones.class);

        /** Un template que corre el callback las veces pedidas y puede fallar con una carrera las primeras. */
        private TransactionTemplate templateQueFalla(int veces) {
            TransactionTemplate template = mock(TransactionTemplate.class);
            AtomicInteger intentos = new AtomicInteger();
            when(template.execute(any())).thenAnswer(invocacion -> {
                TransactionCallback<?> accion = invocacion.getArgument(0);
                if (intentos.incrementAndGet() <= veces) {
                    throw new OptimisticLockingFailureException("perfil", (Throwable) null);
                }
                return accion.doInTransaction(null);
            });
            return template;
        }

        private static ImpactoDonacionDTO dto(UUID idDonacion) {
            ImpactoDonacionDTO dto = new ImpactoDonacionDTO();
            dto.setIdDonacion(idDonacion);
            dto.setFechaEntrega(LocalDateTime.of(2026, 3, 10, 10, 0));
            dto.setCantidadBienes(4);
            dto.setCategoria("INDUMENTARIA");
            dto.setSubCategoria("ROPA");
            dto.setEntidadBeneficiaria("Fundacion");
            dto.setEstado("ENTREGADA");
            return dto;
        }

        private PerfilService servicioCon(TransactionTemplate template) {
            return new PerfilService(
                    repoPerfiles,
                    mock(RepositorioCategorias.class),
                    repoDonaciones,
                    template);
        }

        @Test
        @DisplayName("Perfil tiene @Version: es la raíz del agregado, donde viven progreso, categoría e insignias")
        void perfilTieneVersion() throws Exception {
            Field version = Perfil.class.getDeclaredField("version");

            assertThat(version.getAnnotation(jakarta.persistence.Version.class))
                .as("""
                Sin @Version en Perfil, dos donaciones simultáneas se pisan el progreso y
                pueden duplicar la insignia. Ponerlo en ProgresoMision no serviría: dejaría
                sin cubrir el avance de categoría y el Set de insignias, que son parte del
                mismo agregado.
                """)
                    .isNotNull();
        }

        @Test
        @DisplayName("Categoria también tiene @Version: es el otro leer-modificar-escribir del servicio")
        void categoriaTieneVersion() throws Exception {
            Field version = Categoria.class.getDeclaredField("version");

            assertThat(version.getAnnotation(jakarta.persistence.Version.class))
                    .as("actualizarCategoria lee, arma una nueva y guarda: sin @Version pisa")
                    .isNotNull();
        }

        @Test
        @DisplayName("el @Version no tiene setter: tocarlo a mano rompe la garantía")
        void elVersionNoTieneSetter() throws Exception {
            Field version = Perfil.class.getDeclaredField("version");

            assertThat(version.getAnnotation(lombok.Setter.class))
                    .as("el campo es de Hibernate, igual que el id")
                    .isNull();
        }

        @Test
        @DisplayName("una carrera se reintenta y la donación se aplica igual")
        void unaCarreraSeReintenta() {
            when(repoDonaciones.findById(any())).thenReturn(Optional.empty());
            when(repoPerfiles.findByIdParaDonacion(any()))
                    .thenReturn(Optional.of(new Perfil(UUID.randomUUID(), "Ana")));
            when(repoPerfiles.save(any(Perfil.class))).thenAnswer(inv -> inv.getArgument(0));

            PerfilService service = servicioCon(templateQueFalla(1));

            // La primera vuelta pierde la carrera, la segunda entra limpia.
            assertThat(service.actualizarPerfilImpacto(UUID.randomUUID(), dto(UUID.randomUUID())))
                    .isFalse();

            // save UNA vez: el intento que pierde la carrera falla antes de llegar al save, así
            // que el reintento no deja nada guardado a medias.
            verify(repoPerfiles, times(1)).save(any(Perfil.class));
            verify(repoDonaciones, times(1)).save(any());
        }

        @Test
        @DisplayName("espera con backoff entre reintentos, no en bucle cerrado")
        void esperaAntesDeCadaReintento() {
            when(repoDonaciones.findById(any())).thenReturn(Optional.empty());
            when(repoPerfiles.findByIdParaDonacion(any()))
                    .thenReturn(Optional.of(new Perfil(UUID.randomUUID(), "Ana")));
            when(repoPerfiles.save(any(Perfil.class))).thenAnswer(inv -> inv.getArgument(0));

            List<Integer> esperas = new ArrayList<>();
            PerfilService service = new PerfilService(
                    repoPerfiles,
                    mock(RepositorioCategorias.class),
                    repoDonaciones,
                    templateQueFalla(2)) {
                @Override
                void esperarReintento(int intento) {
                    esperas.add(intento);
                }
            };

            service.actualizarPerfilImpacto(UUID.randomUUID(), dto(UUID.randomUUID()));

            assertThat(esperas).containsExactly(1, 2);
        }

        @Test
        @DisplayName("el retraso está acotado y crece con el intento")
        void elRetrasoEstaAcotado() {
            for (int intento = 1; intento <= 2; intento++) {
                long maximo = intento == 1 ? 40L : 80L;
                for (int muestra = 0; muestra < 20; muestra++) {
                    assertThat(PerfilService.retrasoMilisegundos(intento))
                            .isBetween(20L, maximo);
                }
            }
        }

        @Test
        @DisplayName("dos carreras seguidas también se resuelven: hay margen de reintentos")
        void dosCarrerasResuelven() {
            when(repoDonaciones.findById(any())).thenReturn(Optional.empty());
            when(repoPerfiles.findByIdParaDonacion(any()))
                    .thenReturn(Optional.of(new Perfil(UUID.randomUUID(), "Ana")));
            when(repoPerfiles.save(any(Perfil.class))).thenAnswer(inv -> inv.getArgument(0));

            PerfilService service = servicioCon(templateQueFalla(2));

            assertThat(service.actualizarPerfilImpacto(UUID.randomUUID(), dto(UUID.randomUUID())))
                    .isFalse();

            // Solo el tercer intento llegó a escribir.
            verify(repoPerfiles, times(1)).save(any(Perfil.class));
        }

        @Test
        @DisplayName("agotados los reintentos, la excepción sube para que el handler la mapee a 409")
        void agotadosLosReintentosLaExcepcionSube() {
            when(repoDonaciones.findById(any())).thenReturn(Optional.empty());
            when(repoPerfiles.findByIdParaDonacion(any()))
                    .thenReturn(Optional.of(new Perfil(UUID.randomUUID(), "Ana")));

            PerfilService service = servicioCon(templateQueFalla(99));

            assertThatThrownBy(() ->
                    service.actualizarPerfilImpacto(UUID.randomUUID(), dto(UUID.randomUUID())))
                    .isInstanceOf(OptimisticLockingFailureException.class);
        }

        @Test
        @DisplayName("una excepción que NO es de carrera no se reintenta")
        void unaExcepcionAjenaNoSeReintenta() {
            TransactionTemplate template = mock(TransactionTemplate.class);
            when(template.execute(any())).thenThrow(new IllegalStateException("otra cosa"));

            PerfilService service = servicioCon(template);

            assertThatThrownBy(() ->
                    service.actualizarPerfilImpacto(UUID.randomUUID(), dto(UUID.randomUUID())))
                    .isInstanceOf(IllegalStateException.class);

            verify(template, times(1)).execute(any());
        }

        @Test
        @DisplayName("el handler mapea la carrera a 409, que es el código reintentable por definición")
        void elHandlerMapeaAConglicto() throws Exception {
            Method handler = ar.edu.utn.frba.ddsi.incentivos.exceptions.GlobalExceptionHandler.class
                    .getMethod("manejarConcurrencia", OptimisticLockingFailureException.class);

            var anotacion = handler.getAnnotation(
                    org.springframework.web.bind.annotation.ExceptionHandler.class);

            assertThat(anotacion).isNotNull();
            assertThat(anotacion.value())
                    .as("un 500 haría que el cliente trate el fallo como irrecuperable")
                    .contains(OptimisticLockingFailureException.class);
        }
        }

        @Nested
        @DisplayName("Punto 6: constancia consultada y recalculada en la misma transacción")
        class ConstanciaTransaccional {


        @Test
        @DisplayName("los perfiles se buscan dentro de la transacción que los recalcula")
        void laConsultaCorreDentroDeLaTransaccion() {
            RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
            RepositorioDonaciones repoDonaciones = mock(RepositorioDonaciones.class);

            AtomicBoolean enTransaccion = new AtomicBoolean(false);
            AtomicBoolean consultadaDentroDeLaTransaccion = new AtomicBoolean(false);

            // Un template que corre el callback de verdad y enciende la bandera mientras corre.
            TransactionTemplate template = mock(TransactionTemplate.class);
            when(template.execute(any())).thenAnswer(invocacion -> {
                TransactionCallback<?> accion = invocacion.getArgument(0);
                enTransaccion.set(true);
                try {
                    return accion.doInTransaction(null);
                } finally {
                    enTransaccion.set(false);
                }
            });

            // La consulta captura si la bandera estaba encendida en el momento en que se pidió.
            Perfil perfil = mock(Perfil.class);
            ProgresoMision progreso = mock(ProgresoMision.class);
            Mision mision = mock(Mision.class);
            UUID idUsuario = UUID.randomUUID();
            UUID idMision = UUID.randomUUID();

            when(perfil.getProgresoMisionActual()).thenReturn(progreso);
            when(progreso.getMision()).thenReturn(mision);
            when(mision.getIdMision()).thenReturn(idMision);
            when(perfil.getIdUsuario()).thenReturn(idUsuario);
            when(repoDonaciones.buscarDonacionesQueProgresaronDePerfiles(any(), any()))
                    .thenReturn(List.of());

            when(repoPerfiles.buscarPerfilesConMisionQueRequiereConstancia(any(Pageable.class)))
                    .thenAnswer(invocacion -> {
                        consultadaDentroDeLaTransaccion.set(enTransaccion.get());
                        // Menos de TAMANO_BLOQUE_CONSTANCIA para que el loop termine en la vuelta.
                        return new PageImpl<>(List.of(perfil));
                    });

            PerfilService service = new PerfilService(
                    repoPerfiles,
                    mock(RepositorioCategorias.class),
                    repoDonaciones,
                    template);

            service.evaluarConstanciaPerfiles();

            assertThat(consultadaDentroDeLaTransaccion).isTrue();
        }
        }

        @Nested
        @DisplayName("Alta de perfiles en lote")
        class AltaEnLote {


        private PerfilDonanteDTO perfil(UUID id, String nombre) {
            return new PerfilDonanteDTO(id, nombre, "DONANTE");
        }

        private TransactionTemplate fakeTemplate() {
            return new TransactionTemplate() {
                @Override
                public <T> T execute(TransactionCallback<T> action) {
                    return action.doInTransaction(new SimpleTransactionStatus());
                }
            };
        }

        private PerfilService service(RepositorioPerfiles repoPerfiles,
                                      RepositorioCategorias repoCategorias,
                                      TransactionTemplate template) {
            return new PerfilService(
                    repoPerfiles,
                    repoCategorias,
                    mock(RepositorioDonaciones.class),
                    template);
        }

        private void categoriaBase(RepositorioCategorias repoCategorias) {
            when(repoCategorias.obtenerCategoriaBase()).thenReturn(Optional.of(mock(Categoria.class)));
        }

        @Test
        @DisplayName("Crea los nuevos, saltea los existentes y reporta los rotos sin abortar el lote")
        void creaSalteaYAislaErrores() {
            RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
            RepositorioCategorias repoCategorias = mock(RepositorioCategorias.class);

            UUID nuevo1 = UUID.randomUUID();
            UUID nuevo2 = UUID.randomUUID();
            UUID existente = UUID.randomUUID();
            UUID roto = UUID.randomUUID();

            categoriaBase(repoCategorias);
            when(repoPerfiles.buscarIdsExistentes(any())).thenReturn(List.of(existente));
            when(repoPerfiles.saveAll(any())).thenAnswer(invocacion -> {
                List<Perfil> perfiles = invocacion.getArgument(0);
                if (perfiles.stream().anyMatch(p -> p.getIdUsuario().equals(roto))) {
                    throw new RuntimeException("la base se cayó");
                }
                return perfiles;
            });
            when(repoPerfiles.save(any(Perfil.class))).thenAnswer(invocacion -> {
                Perfil perfil = invocacion.getArgument(0);
                if (perfil.getIdUsuario().equals(roto)) {
                    throw new RuntimeException("la base se cayó");
                }
                return perfil;
            });

            ResultadoLotePerfilesDTO resultado = service(repoPerfiles, repoCategorias, fakeTemplate())
                    .crearPerfilesEnLote(List.of(
                            perfil(nuevo1, "Sofia"),
                            perfil(existente, "Ana"),
                            perfil(nuevo2, "Benjamín"),
                            perfil(roto, "Lucía")));

            assertThat(resultado.getCreados()).isEqualTo(2);
            assertThat(resultado.getYaExistian()).isEqualTo(1);
            assertThat(resultado.getErrores()).hasSize(1);
            assertThat(resultado.getErrores().get(0))
                    .contains(roto.toString())
                    .contains("la base se cayó");
        }

        @Test
        @DisplayName("Reintentar un lote entero ya creado no es un error: todo queda como yaExistian")
        void reintentoCompletoEsIdempotente() {
            RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
            RepositorioCategorias repoCategorias = mock(RepositorioCategorias.class);
            UUID primera = UUID.randomUUID();
            UUID segunda = UUID.randomUUID();

            categoriaBase(repoCategorias);
            when(repoPerfiles.buscarIdsExistentes(any())).thenReturn(List.of(primera, segunda));

            ResultadoLotePerfilesDTO resultado = service(repoPerfiles, repoCategorias, fakeTemplate())
                    .crearPerfilesEnLote(List.of(
                            perfil(primera, "Sofia"),
                            perfil(segunda, "Ana")));

            assertThat(resultado.getCreados()).isZero();
            assertThat(resultado.getYaExistian()).isEqualTo(2);
            assertThat(resultado.getErrores()).isEmpty();
            verify(repoPerfiles, never()).saveAll(any());
        }

        @Test
        @DisplayName("El lote usa una transacción por bloque, no una por ítem")
        void elBloqueUsaUnaSolaTransaccion() {
            RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
            RepositorioCategorias repoCategorias = mock(RepositorioCategorias.class);
            AtomicInteger transacciones = new AtomicInteger();

            TransactionTemplate template = new TransactionTemplate() {
                @Override
                public <T> T execute(TransactionCallback<T> action) {
                    transacciones.incrementAndGet();
                    return action.doInTransaction(new SimpleTransactionStatus());
                }
            };

            categoriaBase(repoCategorias);
            when(repoPerfiles.buscarIdsExistentes(any())).thenReturn(List.of());
            when(repoPerfiles.saveAll(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            service(repoPerfiles, repoCategorias, template).crearPerfilesEnLote(List.of(
                    perfil(UUID.randomUUID(), "Sofia"),
                    perfil(UUID.randomUUID(), "Ana")));

            assertThat(transacciones.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("La categoría base se consulta una sola vez por lote")
        void laCategoriaBaseSeConsultaUnaVez() {
            RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
            RepositorioCategorias repoCategorias = mock(RepositorioCategorias.class);

            categoriaBase(repoCategorias);
            when(repoPerfiles.buscarIdsExistentes(any())).thenReturn(List.of());
            when(repoPerfiles.saveAll(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            service(repoPerfiles, repoCategorias, fakeTemplate()).crearPerfilesEnLote(List.of(
                    perfil(UUID.randomUUID(), "Sofia"),
                    perfil(UUID.randomUUID(), "Ana"),
                    perfil(UUID.randomUUID(), "Benjamín")));

            verify(repoCategorias, times(1)).obtenerCategoriaBase();
        }

        @Test
        @DisplayName("Un id repetido dentro del mismo lote cuenta como existente")
        void idRepetidoDentroDelLoteCuentaComoExistente() {
            RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
            RepositorioCategorias repoCategorias = mock(RepositorioCategorias.class);
            UUID id = UUID.randomUUID();

            categoriaBase(repoCategorias);
            when(repoPerfiles.buscarIdsExistentes(any())).thenReturn(List.of());
            when(repoPerfiles.saveAll(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            ResultadoLotePerfilesDTO resultado = service(repoPerfiles, repoCategorias, fakeTemplate())
                    .crearPerfilesEnLote(List.of(
                            perfil(id, "Sofia"),
                            perfil(id, "Sofia repetida")));

            assertThat(resultado.getCreados()).isEqualTo(1);
            assertThat(resultado.getYaExistian()).isEqualTo(1);
            assertThat(resultado.getErrores()).isEmpty();
        }

        @Test
        @DisplayName("Cuando un ítem lanza DataIntegrityViolationException, no se aborta el lote y se formatea el error")
        void errorDeIntegridadNoTumbaElLoteYSeFormatea() {
            RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
            RepositorioCategorias repoCategorias = mock(RepositorioCategorias.class);

            UUID nuevo = UUID.randomUUID();
            UUID roto = UUID.randomUUID();

            categoriaBase(repoCategorias);
            when(repoPerfiles.buscarIdsExistentes(any())).thenReturn(List.of());
            when(repoPerfiles.saveAll(any())).thenThrow(
                    new DataIntegrityViolationException("violación de constraint"));
            when(repoPerfiles.save(any(Perfil.class))).thenAnswer(invocacion -> {
                Perfil perfil = invocacion.getArgument(0);
                if (perfil.getIdUsuario().equals(roto)) {
                    throw new DataIntegrityViolationException("violación de constraint");
                }
                return perfil;
            });

            ResultadoLotePerfilesDTO resultado = service(repoPerfiles, repoCategorias, fakeTemplate())
                    .crearPerfilesEnLote(List.of(
                            perfil(nuevo, "Sofia"),
                            perfil(roto, "Lucía")));

            assertThat(resultado.getCreados()).isEqualTo(1);
            assertThat(resultado.getYaExistian()).isZero();
            assertThat(resultado.getErrores()).hasSize(1);
            assertThat(resultado.getErrores().get(0))
                    .contains(roto.toString())
                    .contains("Error de integridad de datos al persistir el perfil");
        }
        }

        @Nested
        @DisplayName("PerfilService: idempotencia de la ingesta de donaciones")
        class Idempotencia {


        private static final UUID USUARIO = UUID.randomUUID();
        private static final UUID ID_DONACION = UUID.randomUUID();
        private static final LocalDateTime FECHA = LocalDateTime.of(2026, 3, 10, 10, 0);

        private RepositorioPerfiles repoPerfiles;
        private RepositorioDonaciones repoDonaciones;
        private PerfilService service;

        @BeforeEach
        void setUp() {
            repoPerfiles = mock(RepositorioPerfiles.class);
            repoDonaciones = mock(RepositorioDonaciones.class);
            // El template corre el callback sin transacción real: acá se prueba la lógica del service.
            TransactionTemplate template = mock(TransactionTemplate.class);
            when(template.execute(any())).thenAnswer(invocacion -> {
                org.springframework.transaction.support.TransactionCallback<?> accion = invocacion.getArgument(0);
                return accion.doInTransaction(null);
            });

            service = new PerfilService(
                    repoPerfiles,
                    mock(RepositorioCategorias.class),
                    repoDonaciones,
                    template
            );
        }

        private ImpactoDonacionDTO dto() {
            ImpactoDonacionDTO dto = new ImpactoDonacionDTO();
            dto.setIdDonacion(ID_DONACION);
            dto.setFechaEntrega(FECHA);
            dto.setCantidadBienes(4);
            dto.setCategoria("INDUMENTARIA");
            dto.setSubCategoria("ROPA");
            dto.setEntidadBeneficiaria("Fundacion");
            dto.setEstado("ENTREGADA");
            return dto;
        }

        /** La fila que quedó guardada la primera vez. */
        private ImpactoDonacion donacionGuardada(boolean completoMision) {
            ImpactoDonacion donacion = new ImpactoDonacion(
                    ID_DONACION, USUARIO,
                    "Fundacion", 4, FECHA, "INDUMENTARIA", "ROPA", "ENTREGADA");
            donacion.registrarSiCompletoMision(completoMision);
            return donacion;
        }

        /** Un perfil sin misión activa: alcanza para verificar que la donación se guarda. */
        private void hayPerfilRegistrado() {
            when(repoPerfiles.findByIdParaDonacion(USUARIO)).thenReturn(Optional.of(new Perfil(USUARIO, "Ana")));
        }

        @Test
        @DisplayName("una donación repetida no se vuelve a procesar")
        void unaDonacionRepetidaNoSeProcesa() {
            when(repoDonaciones.findById(ID_DONACION))
                    .thenReturn(Optional.of(donacionGuardada(true)));

            boolean resultado = service.actualizarPerfilImpacto(USUARIO, dto());

            // Lo importante es que no se guarda una segunda vez ni se toca el perfil: si se
            // reprocesara, volvería a aplicar la regla y el progreso quedaría inflado.
            assertThat(resultado).isTrue();
            verify(repoDonaciones, never()).save(any());
            verify(repoPerfiles, never()).save(any());
        }

        @Test
        @DisplayName("el reintento devuelve el mismo resultado que la primera vez")
        void elReintentoDevuelveElMismoResultado() {
            when(repoDonaciones.findById(ID_DONACION))
                    .thenReturn(Optional.of(donacionGuardada(true)));
            boolean primeraVez = service.actualizarPerfilImpacto(USUARIO, dto());

            when(repoDonaciones.findById(ID_DONACION))
                    .thenReturn(Optional.of(donacionGuardada(true)));
            Boolean segundaVez = service.actualizarPerfilImpacto(USUARIO, dto());

            // Un endpoint idempotente no puede devolver algo distinto la segunda vez: el
            // cliente ya recibió true, y si ahora recibiera false lo tomaría por un fallo.
            assertThat(segundaVez).isEqualTo(primeraVez).isTrue();
        }

        @Test
        @DisplayName("el reintento de una donación que NO completó misión también es idempotente")
        void elReintentoDeUnaDonacionIncompletaTambienEsIdempotente() {
            when(repoDonaciones.findById(ID_DONACION))
                    .thenReturn(Optional.of(donacionGuardada(false)));

            boolean resultado = service.actualizarPerfilImpacto(USUARIO, dto());

            assertThat(resultado).isFalse();
            verify(repoDonaciones, never()).save(any());
        }

        @Test
        @DisplayName("una donación nueva sí se procesa y guarda con el id de origen")
        void unaDonacionNuevaSiSeProcesa() {
            hayPerfilRegistrado();
            when(repoDonaciones.findById(ID_DONACION)).thenReturn(Optional.empty());

            service.actualizarPerfilImpacto(USUARIO, dto());

            verify(repoDonaciones, times(1)).save(any(ImpactoDonacion.class));
        }

        @Test
        @DisplayName("la donación reportada por el servicio de origen guarda su id sin traducirlo")
        void elIdGuardadoEsElDeOrigen() {
            hayPerfilRegistrado();
            when(repoDonaciones.findById(ID_DONACION)).thenReturn(Optional.empty());

            service.actualizarPerfilImpacto(USUARIO, dto());

            // La primary key local ES el id de la donación en donaciones-service: eso es lo
            // que hace que el findById sirva para detectar el reintento.
            var captor = org.mockito.ArgumentCaptor.forClass(ImpactoDonacion.class);
            verify(repoDonaciones).save(captor.capture());
            assertThat(captor.getValue().getIdDonacion()).isEqualTo(ID_DONACION);
        }

        @Test
        @DisplayName("dos donaciones distintas del mismo donante se procesan por separado")
        void dosDonacionesDistintasSeProcesanPorSeparado() {
            hayPerfilRegistrado();
            when(repoDonaciones.findById(ID_DONACION)).thenReturn(Optional.empty());

            ImpactoDonacionDTO otra = dto();
            otra.setIdDonacion(UUID.randomUUID());
            otra.setCantidadBienes(9);
            UUID idOtra = UUID.randomUUID();

            when(repoDonaciones.findById(idOtra)).thenReturn(Optional.empty());

            service.actualizarPerfilImpacto(USUARIO, dto());
            service.actualizarPerfilImpacto(USUARIO, otra);

            // Que el id sea el de origen evita el problema opuesto: tratar como reintento dos
            // dones realmente distintas del mismo donante.
            verify(repoDonaciones, times(2)).save(any(ImpactoDonacion.class));
        }
        }

        @Nested
        @DisplayName("Punto 54: Soft Delete y minimización de Perfil preservando ImpactoDonacion")
        class SoftDelete {


        private static final UUID ADMIN_ID = UUID.randomUUID();
        private static final UUID USER_ID = UUID.randomUUID();

        private Map<UUID, Perfil> storePerfiles;
        private PerfilService service;
        private Perfil perfilActivo;

        @BeforeEach
        void setUp() {
            storePerfiles = new HashMap<>();

            perfilActivo = new Perfil(USER_ID, "DonanteActivo");
            Categoria categoria = new Categoria("Oro", null, 1, new ArrayList<>());
            perfilActivo.iniciarEn(categoria);

            Insignia insignia = new Insignia("Medalla 1", "Descripcion", "http://imagen");
            perfilActivo.getInsigniasObtenidas().add(new InsigniaObtenida(perfilActivo, insignia));

            storePerfiles.put(USER_ID, perfilActivo);

            RepositorioPerfiles fakeRepoPerfiles = new FakeRepositorioPerfiles(storePerfiles);
            TransactionTemplate fakeTxTemplate = new TransactionTemplate() {
                @Override
                public <T> T execute(TransactionCallback<T> action) {
                    return action.doInTransaction(new SimpleTransactionStatus());
                }
            };

            service = new PerfilService(
                    fakeRepoPerfiles,
                    null,
                    null, // RepositorioDonaciones nunca debe ser llamado para borrar
                    fakeTxTemplate
            );
        }

        @Test
        @DisplayName("eliminarPerfil por admin aplica soft delete y minimiza datos sin borrar la entidad")
        void eliminarPerfilAplicaSoftDeleteYMinimizacion() {
            assertThat(perfilActivo.isActivo()).isTrue();
            assertThat(perfilActivo.getNombreUsuario()).isEqualTo("DonanteActivo");
            assertThat(perfilActivo.getInsigniasObtenidas()).isNotEmpty();

            service.eliminarPerfil(USER_ID, ADMIN_ID);

            assertThat(storePerfiles.containsKey(USER_ID)).isTrue();
            Perfil perfilGuardado = storePerfiles.get(USER_ID);

            assertThat(perfilGuardado.isActivo()).isFalse();
            assertThat(perfilGuardado.getNombreUsuario()).isNull();
            assertThat(perfilGuardado.getCategoriaActual()).isNull();
            assertThat(perfilGuardado.getProgresoMisionActual()).isNull();
            assertThat(perfilGuardado.getInsigniasObtenidas()).isEmpty();
        }

        @Test
        @DisplayName("eliminarPerfil sobre un perfil ya inactivo lanza InexistenteException (404)")
        void eliminarPerfilYaInactivoLanza404() {
            service.eliminarPerfil(USER_ID, ADMIN_ID);

            assertThatThrownBy(() -> service.eliminarPerfil(USER_ID, ADMIN_ID))
                    .isInstanceOf(InexistenteException.class);
        }

        @Test
        @DisplayName("eliminarPerfilPorBajaDeDonante es idempotente y no falla con perfil inexistente o inactivo")
        void eliminarPerfilPorBajaDeDonanteIdempotente() {
            service.eliminarPerfilPorBajaDeDonante(USER_ID);
            assertThat(perfilActivo.isActivo()).isFalse();

            // Segunda llamada no falla ni produce efectos secundarios
            service.eliminarPerfilPorBajaDeDonante(USER_ID);
            service.eliminarPerfilPorBajaDeDonante(UUID.randomUUID());
        }

        @Test
        @DisplayName("consultas sobre perfil inactivo responden InexistenteException (404)")
        void consultasSobrePerfilInactivoResponden404() {
            service.eliminarPerfil(USER_ID, ADMIN_ID);

            assertThatThrownBy(() -> service.buscarPorIdUsuario(USER_ID))
                    .isInstanceOf(InexistenteException.class);

            assertThatThrownBy(() -> service.obtenerPerfilPublico(USER_ID))
                    .isInstanceOf(InexistenteException.class);

            assertThatThrownBy(() -> service.obtenerInsigniasPorIdUsuario(USER_ID, PageRequest.of(0, 10)))
                    .isInstanceOf(InexistenteException.class);

            assertThatThrownBy(() -> service.obtenerMisionPorIdUsuario(USER_ID))
                    .isInstanceOf(InexistenteException.class);

            assertThatThrownBy(() -> service.actualizarDatosPerfil(USER_ID, ADMIN_ID, new PerfilDTO()))
                    .isInstanceOf(InexistenteException.class);
        }

        @Test
        @DisplayName("actualizarPerfilPorModificacionDeDonante no altera perfil inactivo")
        void modificacionExternaNoAlteraPerfilInactivo() {
            service.eliminarPerfil(USER_ID, ADMIN_ID);

            service.actualizarPerfilPorModificacionDeDonante(USER_ID, new PerfilDonanteDTO(USER_ID, "NuevoNombre", null));

            assertThat(perfilActivo.getNombreUsuario()).isNull();
        }

        @SuppressWarnings("deprecation")
        private static class FakeRepositorioPerfiles implements RepositorioPerfiles {
            private final Map<UUID, Perfil> store;

            FakeRepositorioPerfiles(Map<UUID, Perfil> store) {
                this.store = store;
            }

            @Override
            public Optional<Perfil> findById(UUID id) {
                return Optional.ofNullable(store.get(id));
            }

            @Override
            public Optional<Perfil> findByIdParaDonacion(UUID id) {
                return findById(id);
            }

            @Override
            public List<UUID> buscarIdsExistentes(Collection<UUID> ids) {
                return ids.stream().filter(store::containsKey).toList();
            }

            @Override
            public boolean existsById(UUID id) {
                return store.containsKey(id);
            }

            @Override
            public <S extends Perfil> S save(S entity) {
                store.put(entity.getIdUsuario(), entity);
                return entity;
            }

            @Override
            public void deleteById(UUID id) {
                throw new UnsupportedOperationException("No se debe borrar físicamente el perfil");
            }

            @Override
            public Page<InsigniaObtenida> paginaInsigniasPorIdUsuario(UUID idUsuario, Pageable pageable) {
                return new PageImpl<>(List.of());
            }

            @Override
            public Optional<ProgresoMision> obtenerProgresoMisionPorIdUsuario(UUID idUsuario) {
                return Optional.empty();
            }

            @Override
            public long countByCategoriaActual(Categoria categoria) { return 0; }
            @Override
            public long countByProgresoMisionActualMision(Mision mision) { return 0; }
            @Override
            public long countByInsigniasObtenidasInsignia(Insignia insignia) { return 0; }
            @Override
            public List<Perfil> findAllByCategoriaActual(Categoria categoria) { return List.of(); }
            @Override
            public Page<Perfil> findByCategoriaActual(Categoria categoria, Pageable pageable) { return Page.empty(); }
            @Override
            public List<Perfil> findAllByMisionActual(UUID idMision) { return List.of(); }
            @Override
            public Page<Perfil> findByMisionActualPaginado(UUID idMision, Pageable pageable) { return Page.empty(); }
            @Override
            public Page<Perfil> buscarPerfilesConMisionQueRequiereConstancia(Pageable pageable) { return Page.empty(); }
            @Override
            public Slice<RankingFilaDTO> calcularRankingMensual(java.time.LocalDateTime inicio, java.time.LocalDateTime fin, Pageable pageable) { return Page.empty(); }

            @Override public void flush() {}
            @Override public <S extends Perfil> S saveAndFlush(S entity) { return save(entity); }
            @Override public <S extends Perfil> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
            @Override public void deleteAllInBatch(Iterable<Perfil> entities) {}
            @Override public void deleteAllByIdInBatch(Iterable<UUID> uuids) {}
            @Override public void deleteAllInBatch() {}
            @Override public Perfil getOne(UUID uuid) { return store.get(uuid); }
            @Override public Perfil getById(UUID uuid) { return store.get(uuid); }
            @Override public Perfil getReferenceById(UUID uuid) { return store.get(uuid); }
            @Override public <S extends Perfil> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
            @Override public <S extends Perfil> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
            @Override public <S extends Perfil> List<S> findAll(org.springframework.data.domain.Example<S> example, Sort sort) { return List.of(); }
            @Override public <S extends Perfil> Page<S> findAll(org.springframework.data.domain.Example<S> example, Pageable pageable) { return Page.empty(); }
            @Override public <S extends Perfil> long count(org.springframework.data.domain.Example<S> example) { return 0; }
            @Override public <S extends Perfil> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
            @Override public <S extends Perfil, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
            @Override public <S extends Perfil> List<S> saveAll(Iterable<S> entities) { return List.of(); }
            @Override public List<Perfil> findAll() { return new ArrayList<>(store.values()); }
            @Override public List<Perfil> findAllById(Iterable<UUID> uuids) { return List.of(); }
            @Override public long count() { return store.size(); }
            @Override public void delete(Perfil entity) { throw new UnsupportedOperationException(); }
            @Override public void deleteAllById(Iterable<? extends UUID> uuids) { throw new UnsupportedOperationException(); }
            @Override public void deleteAll(Iterable<? extends Perfil> entities) { throw new UnsupportedOperationException(); }
            @Override public void deleteAll() { throw new UnsupportedOperationException(); }
            @Override public List<Perfil> findAll(Sort sort) { return List.of(); }
            @Override public Page<Perfil> findAll(Pageable pageable) { return Page.empty(); }
        }
        }

    @Nested
        @DisplayName("Punto 8: la categoría es visible públicamente")
        class PerfilPublico {

            private RepositorioPerfiles repoPerfiles;
            private PerfilService perfilService;

            @BeforeEach
            void setUpPerfil() {
                repoPerfiles = mock(RepositorioPerfiles.class);
                perfilService = new PerfilService(
                        repoPerfiles,
                        mock(RepositorioCategorias.class),
                        mock(RepositorioDonaciones.class),
                        mock(TransactionTemplate.class));
            }

            @Test
            @DisplayName("devuelve el nombre de usuario y el de su categoría")
            void devuelveNombreYCategoria() {
                UUID idUsuario = UUID.randomUUID();
                Categoria categoria = new Categoria("Transformador", null, 1, List.of());

                Perfil perfil = new Perfil(idUsuario, "Ana");
                perfil.iniciarEn(categoria);
                when(repoPerfiles.findById(idUsuario)).thenReturn(Optional.of(perfil));

                PerfilPublicoDTO dto = perfilService.obtenerPerfilPublico(idUsuario);

                assertThat(dto.getNombreUsuario()).isEqualTo("Ana");
                assertThat(dto.getNombreCategoria()).isEqualTo("Transformador");
            }

            @Test
            @DisplayName("un perfil sin categoría responde igual, con la categoría en null")
            void sinCategoriaRespondeConNull() {
                UUID idUsuario = UUID.randomUUID();
                Perfil perfil = new Perfil(idUsuario, "Beto");
                when(repoPerfiles.findById(idUsuario)).thenReturn(Optional.of(perfil));

                PerfilPublicoDTO dto = perfilService.obtenerPerfilPublico(idUsuario);

                // El donante existe, asi que no es un 404: su nombre tiene que poder verse.
                assertThat(dto.getNombreUsuario()).isEqualTo("Beto");
                assertThat(dto.getNombreCategoria()).isNull();
            }

            @Test
            @DisplayName("si el donante no existe responde 404")
            void siNoExisteResponde404() {
                UUID idUsuario = UUID.randomUUID();
                when(repoPerfiles.findById(idUsuario)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> perfilService.obtenerPerfilPublico(idUsuario))
                        .isInstanceOf(InexistenteException.class);
            }
        }

    @Nested
        @DisplayName("Editar y borrar perfiles")
        class EscriturasDePerfil {

            private static final UUID ADMIN = UUID.randomUUID();

            private RepositorioPerfiles repoPerfiles;
            private PerfilService perfilService;

            @BeforeEach
            void setUpPerfil() {
                repoPerfiles = mock(RepositorioPerfiles.class);
                perfilService = new PerfilService(
                        repoPerfiles,
                        mock(RepositorioCategorias.class),
                        mock(RepositorioDonaciones.class),
                        mock(TransactionTemplate.class));
            }

            

            @Test
            @DisplayName("un nombre en blanco no reemplaza al existente")
            void nombreEnBlancoNoReemplaza() {
                UUID idUsuario = UUID.randomUUID();
                Perfil perfil = new Perfil(idUsuario, "Ana");
                when(repoPerfiles.findById(idUsuario)).thenReturn(Optional.of(perfil));
                when(repoPerfiles.save(any(Perfil.class)))
                        .thenAnswer(invoc -> invoc.getArgument(0));

                perfilService.actualizarDatosPerfil(
                        idUsuario, ADMIN, new PerfilDTO("   ", null, null, null));

                assertThat(perfil.getNombreUsuario()).isEqualTo("Ana");
            }

            @Test
            @DisplayName("un nombre con espacios se guarda recortado")
            void nombreConEspaciosSeRecorta() {
                UUID idUsuario = UUID.randomUUID();
                Perfil perfil = new Perfil(idUsuario, "Ana");
                when(repoPerfiles.findById(idUsuario)).thenReturn(Optional.of(perfil));
                when(repoPerfiles.save(any(Perfil.class)))
                        .thenAnswer(invoc -> invoc.getArgument(0));

                perfilService.actualizarDatosPerfil(
                        idUsuario, ADMIN, new PerfilDTO("  Ana  ", null, null, null));

                assertThat(perfil.getNombreUsuario()).isEqualTo("Ana");
            }

            

            

            
        }
}
