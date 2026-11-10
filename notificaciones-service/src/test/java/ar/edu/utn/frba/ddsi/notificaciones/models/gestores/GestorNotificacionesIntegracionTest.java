package ar.edu.utn.frba.ddsi.notificaciones.models.gestores;

import ar.edu.utn.frba.ddsi.notificaciones.messaging.ProductorNotificaciones;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.RepositorioNotificaciones;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestorNotificacionesIntegracionTest {

    static class StubRepositorio implements RepositorioNotificaciones {
        Map<UUID, Notificacion> tabla = new HashMap<>();
        Map<String, Notificacion> tablaPorIdSolicitud = new HashMap<>();
        int llamadasSave = 0;

        @Override
        public Optional<Notificacion> findById(UUID id) {
            return Optional.ofNullable(tabla.get(id));
        }

        @Override
        public Optional<Notificacion> findByIdSolicitud(String idSolicitud) {
            return Optional.ofNullable(tablaPorIdSolicitud.get(idSolicitud));
        }

        @Override
        public <S extends Notificacion> S save(S entity) {
            llamadasSave++;
            tabla.put(entity.getId(), entity);
            if (entity.getIdSolicitud() != null) {
                tablaPorIdSolicitud.put(entity.getIdSolicitud(), entity);
            }
            return entity;
        }

        @Override public void flush() {}
        @Override public <S extends Notificacion> S saveAndFlush(S entity) { return null; }
        @Override public <S extends Notificacion> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<Notificacion> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<UUID> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public Notificacion getOne(UUID id) { return null; }
        @Override public Notificacion getById(UUID id) { return null; }
        @Override public Notificacion getReferenceById(UUID id) { return null; }
        @Override public <S extends Notificacion> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends Notificacion> List<S> findAll(Example<S> example) { return null; }
        @Override public <S extends Notificacion> List<S> findAll(Example<S> example, Sort sort) { return null; }
        @Override public <S extends Notificacion> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends Notificacion> long count(Example<S> example) { return 0; }
        @Override public <S extends Notificacion> boolean exists(Example<S> example) { return false; }
        @Override public <S extends Notificacion, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends Notificacion> List<S> saveAll(Iterable<S> entities) { return null; }
        @Override public boolean existsById(UUID id) { return false; }
        @Override public List<Notificacion> findAll() { return null; }
        @Override public List<Notificacion> findAllById(Iterable<UUID> ids) { return null; }
        @Override public long count() { return 0; }
        @Override public void deleteById(UUID id) {}
        @Override public void delete(Notificacion entity) {}
        @Override public void deleteAllById(Iterable<? extends UUID> ids) {}
        @Override public void deleteAll(Iterable<? extends Notificacion> entities) {}
        @Override public void deleteAll() {}
        @Override public List<Notificacion> findAll(Sort sort) { return null; }
        @Override public Page<Notificacion> findAll(Pageable pageable) { return null; }

        @Override
        public Page<Notificacion> findByDireccionDeContactoIgnoreCase(String direccionDeContacto, Pageable pageable) {
            return filtrar(direccionDeContacto, null, pageable);
        }

        @Override
        public Page<Notificacion> findByDireccionDeContactoIgnoreCaseAndEstado(String direccionDeContacto,
                                                                               EstadoNotificacion estado,
                                                                               Pageable pageable) {
            return filtrar(direccionDeContacto, estado, pageable);
        }

        private Page<Notificacion> filtrar(String direccionDeContacto, EstadoNotificacion estado, Pageable pageable) {
            List<Notificacion> filtradas = tabla.values().stream()
                    .filter(n -> n.getDireccionDeContacto() != null
                            && n.getDireccionDeContacto().equalsIgnoreCase(direccionDeContacto))
                    .filter(n -> estado == null || n.getEstado() == estado)
                    .sorted(java.util.Comparator.comparing(Notificacion::getFechaCreacion,
                            java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                    .toList();
            int from = (int) Math.min(pageable.getOffset(), filtradas.size());
            int to = Math.min(from + pageable.getPageSize(), filtradas.size());
            return new org.springframework.data.domain.PageImpl<>(filtradas.subList(from, to), pageable, filtradas.size());
        }
    }

    static class StubProductor extends ProductorNotificaciones {
        int llamadasEnviar = 0;

        public StubProductor() {
            super(null);
        }

        @Override
        public void enviar(Notificacion notificacion) {
            llamadasEnviar++;
        }
    }

    private StubRepositorio repositorio;
    private StubProductor productor;
    private GestorNotificaciones gestor;

    @BeforeEach
    void setUp() {
        repositorio = new StubRepositorio();
        productor = new StubProductor();
        gestor = new GestorNotificaciones(repositorio, null, productor);
    }

    @Test
    @DisplayName("crearNotificacion asigna idSolicitud y persiste correctamente")
    void crearNotificacionAsignaIdSolicitud() {
        Notificacion notificacion = gestor.crearNotificacion(
                "solicitud-abc-123", "email", "ana@test.com", "Asunto", "Cuerpo");

        assertNotNull(notificacion);
        assertEquals("solicitud-abc-123", notificacion.getIdSolicitud());
        assertEquals(1, repositorio.llamadasSave);
        assertTrue(repositorio.findByIdSolicitud("solicitud-abc-123").isPresent());
    }

    @Test
    @DisplayName("enviarSolicitudDeNotificacion con idSolicitud duplicada no crea ni persiste segunda entidad")
    void enviarSolicitudIdempotenteNoDuplicaPersistencia() {
        // Primera solicitud
        gestor.enviarSolicitudDeNotificacion("solicitud-dup-1", "email", "ana@test.com", "Asunto", "Cuerpo");
        assertEquals(1, repositorio.llamadasSave);

        // Segunda solicitud con el mismo idSolicitud
        gestor.enviarSolicitudDeNotificacion("solicitud-dup-1", "email", "ana@test.com", "Asunto", "Cuerpo");
        // No debe haberse llamado a save nuevamente
        assertEquals(1, repositorio.llamadasSave);
    }

    @Test
    @DisplayName("listarPorDestinatario devuelve solo las notificaciones del destinatario, más recientes primero")
    void listarPorDestinatarioFiltraYOrdenaDescendente() {
        Notificacion primera = gestor.crearNotificacion("l-1", "email", "ANA@test.com", "Primera", "Cuerpo 1");
        primera.setFechaCreacion(java.time.LocalDateTime.now().minusHours(3));
        Notificacion segunda = gestor.crearNotificacion("l-2", "email", "ana@test.com", "Segunda", "Cuerpo 2");

        gestor.crearNotificacion("l-3", "email", "otra@test.com", "Otra", "Cuerpo 3");

        java.util.List<Notificacion> resultado = gestor
                .listarPorDestinatario("ana@test.com", null,
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .getContent();

        assertEquals(2, resultado.size());
        assertEquals("Segunda", resultado.get(0).getMensaje().getAsunto());
        assertEquals("Primera", resultado.get(1).getMensaje().getAsunto());
    }

    @Test
    @DisplayName("listarPorDestinatario con estado filtra además por estado")
    void listarPorDestinatarioFiltraPorEstado() {
        Notificacion fallida = gestor.crearNotificacion("e-1", "email", "ana@test.com", "Fallida", "Cuerpo 1");
        fallida.marcarFallida();
        gestor.crearNotificacion("e-2", "email", "ana@test.com", "Pendiente", "Cuerpo 2");

        java.util.List<Notificacion> fallidas = gestor
                .listarPorDestinatario("ana@test.com", EstadoNotificacion.FALLIDA,
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .getContent();

        assertEquals(1, fallidas.size());
        assertEquals("Fallida", fallidas.get(0).getMensaje().getAsunto());
    }
}
