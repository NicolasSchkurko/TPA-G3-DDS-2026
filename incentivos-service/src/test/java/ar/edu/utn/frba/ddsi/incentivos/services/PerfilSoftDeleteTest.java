package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.PerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilPublicoDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.InsigniaObtenida;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.ValidadorAdmin;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@DisplayName("Punto 54: Soft Delete y minimización de Perfil preservando ImpactoDonacion")
class PerfilSoftDeleteTest {

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
        ValidadorAdmin fakeValidadorAdmin = new ValidadorAdmin(null) {
            @Override
            public void verificarPermisos(UUID idAdmin) {
                if (!ADMIN_ID.equals(idAdmin)) {
                    throw new SecurityException("No autorizado");
                }
            }
        };

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
                fakeTxTemplate,
                fakeValidadorAdmin
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
        public List<Perfil> findAllByMisionActual(UUID idMision) { return List.of(); }
        @Override
        public Page<Perfil> buscarPerfilesConMisionQueRequiereConstancia(Pageable pageable) { return Page.empty(); }
        @Override
        public List<Object[]> calcularRankingMensual(java.time.LocalDateTime inicio, java.time.LocalDateTime fin, Pageable pageable) { return List.of(); }

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
