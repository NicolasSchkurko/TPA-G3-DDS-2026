package ar.edu.utn.frba.ddsi.notificaciones.messaging;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.dto.Notificaciones.SolicitudNotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories.NotificacionReintentable;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories.RepositorioNotificaciones;
import ar.edu.utn.frba.ddsi.notificaciones.services.NotificadorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsumidorNotificacionesTest {

    private StubServicio servicio;
    private StubRepositorio repositorio;
    private ObjectMapper objectMapper;
    private ConsumidorNotificaciones consumidor;

    static class StubServicio extends NotificadorService {
        String ultimoMedio;
        String ultimaDireccion;
        String ultimoAsunto;
        String ultimoCuerpo;
        String ultimoIdSolicitud;
        String ultimaUrlMapa;
        boolean lanzarErrorEnSolicitud = false;
        RuntimeException errorEnviarNotificacion = null;
        int llamadasEnviarNotificacion = 0;
        int llamadasEnviarSolicitud = 0;
        int llamadasReclamarEnvio = 0;
        int llamadasMarcarEnviada = 0;
        int llamadasMarcarFallida = 0;
        EstadoNotificacion estadoAlEnviar;
        Notificacion ultimaNotificacionDespachada;
        final RepositorioNotificaciones repo;

        public StubServicio(RepositorioNotificaciones repo) {
            super(repo, null, null, null, null);
            this.repo = repo;
        }

        @Override
        public UUID procesarSolicitudDeNotificacion(SolicitudNotificacionDTO dto) {
            if (lanzarErrorEnSolicitud) {
                throw new DataAccessResourceFailureException("MySQL fuera de línea");
            }
            this.llamadasEnviarSolicitud++;
            this.ultimoIdSolicitud = dto.getIdSolicitud();
            this.ultimoMedio = dto.getMedioDeContacto();
            this.ultimaDireccion = dto.getDireccionDeContacto();
            this.ultimoAsunto = dto.getAsuntoMensaje();
            this.ultimoCuerpo = dto.getCuerpoMensaje();
            this.ultimaUrlMapa = dto.getUrlMapa();
            Notificacion n = Notificacion.crear(dto.getIdSolicitud(), dto.getMedioDeContacto(),
                    dto.getDireccionDeContacto(), dto.getAsuntoMensaje(),
                    dto.getCuerpoMensaje(), dto.getUrlMapa());
            repo.save(n);
            return n.getId();
        }

        @Override
        public Optional<Notificacion> reclamarParaEnvio(UUID id, String medio, String direccion) {
            llamadasReclamarEnvio++;
            Notificacion notificacion = repo.findById(id).orElse(null);
            if (notificacion == null
                    || notificacion.getEstado() == EstadoNotificacion.ENVIADA
                    || notificacion.getEstado() == EstadoNotificacion.ENVIANDO) {
                return Optional.empty();
            }
            if (medio != null && !medio.isBlank()) {
                notificacion.setTipoMedioDeContacto(medio);
            }
            if (direccion != null && !direccion.isBlank()) {
                notificacion.setDireccionDeContacto(direccion);
            }
            notificacion.marcarEnviando(LocalDateTime.now());
            repo.save(notificacion);
            return Optional.of(notificacion);
        }

        @Override
        public void enviarNotificacion(Notificacion notificacion, String tipoMedio, String direccion) {
            this.llamadasEnviarNotificacion++;
            this.ultimoMedio = tipoMedio;
            this.ultimaDireccion = direccion;
            this.ultimaNotificacionDespachada = notificacion;
            this.estadoAlEnviar = notificacion.getEstado();
            if (errorEnviarNotificacion != null) {
                throw errorEnviarNotificacion;
            }
        }

        @Override
        public boolean marcarEnviada(UUID id, UUID claimId) {
            llamadasMarcarEnviada++;
            Notificacion notificacion = repo.findById(id).orElse(null);
            if (notificacion == null
                    || !java.util.Objects.equals(notificacion.getClaimId(), claimId)) {
                return false;
            }
            notificacion.marcarEnviada();
            return true;
        }

        @Override
        public boolean marcarFallida(UUID id, UUID claimId) {
            llamadasMarcarFallida++;
            Notificacion notificacion = repo.findById(id).orElse(null);
            if (notificacion == null
                    || !java.util.Objects.equals(notificacion.getClaimId(), claimId)) {
                return false;
            }
            notificacion.marcarFallida();
            return true;
        }
    }

    static class StubRepositorio implements RepositorioNotificaciones {
        Map<UUID, Notificacion> tabla = new HashMap<>();
        Map<String, Notificacion> tablaPorIdSolicitud = new HashMap<>();

        @Override
        public Optional<Notificacion> findById(UUID id) {
            return Optional.ofNullable(tabla.get(id));
        }

        @Override
        public Optional<Notificacion> findByIdParaEnvio(UUID id) {
            return findById(id);
        }

        @Override
        public Optional<Notificacion> findByIdSolicitud(String idSolicitud) {
            return Optional.ofNullable(tablaPorIdSolicitud.get(idSolicitud));
        }

        @Override
        public List<NotificacionReintentable> findParaReintentar(
                EstadoNotificacion estado, java.time.LocalDateTime limite, int maxIntentos,
                org.springframework.data.domain.Pageable pageable) {
            return List.of();
        }

        @Override
        public int reclamarParaReintento(
                UUID id, EstadoNotificacion estado, int esperados, int maxIntentos) {
            return 0;
        }

        @Override
        public int compensarReintentoFallido(UUID id, EstadoNotificacion estado) {
            return 0;
        }

        @Override
        public int marcarEnviadaSiEnviando(UUID id, UUID claimId, LocalDateTime ahora,
                                           EstadoNotificacion enviada, EstadoNotificacion enviando) {
            return 0;
        }

        @Override
        public int marcarFallidaSiEnviando(UUID id, UUID claimId,
                                           EstadoNotificacion fallida, EstadoNotificacion enviando) {
            return 0;
        }

        @Override
        public int liberarEnviosColgados(EstadoNotificacion pendiente,
                                         EstadoNotificacion enviando, LocalDateTime limite) {
            return 0;
        }

        @Override
        public <S extends Notificacion> S save(S entity) {
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

        @Override public Page<Notificacion> findByEstado(
                EstadoNotificacion estado, Pageable pageable) { return Page.empty(pageable); }
        @Override public Page<Notificacion> buscar(
                EstadoNotificacion estado, Pageable pageable) { return Page.empty(pageable); }
    }

    @BeforeEach
    void setUp() {
        repositorio = new StubRepositorio();
        servicio = new StubServicio(repositorio);
        objectMapper = new ObjectMapper();
        consumidor = new ConsumidorNotificaciones(servicio, objectMapper);
    }

    private Message crearMensajeAmqp(String json, String routingKey) {
        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey(routingKey);
        return new Message(json.getBytes(StandardCharsets.UTF_8), properties);
    }

    @Test
    @DisplayName("Punto 30: Solicitud sin asunto o sin cuerpo se rechaza a DLQ con AmqpRejectAndDontRequeueException")
    void solicitudSinAsuntoSeRechazaADeadLetter() {
        String jsonIncompleto = """
                {
                  "medioDeContacto": "email",
                  "direccionDeContacto": "juan@test.com",
                  "cuerpoMensaje": "Cuerpo sin asunto"
                }
                """;
        Message mensaje = crearMensajeAmqp(jsonIncompleto, "notificaciones.donacion");

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumidor.recibir(mensaje));
        assertNull(servicio.ultimoAsunto);
    }

    @Test
    @DisplayName("Punto 30: Solicitud con cuerpo en blanco se rechaza a DLQ con AmqpRejectAndDontRequeueException")
    void solicitudConCuerpoEnBlancoSeRechazaADeadLetter() {
        String jsonIncompleto = """
                {
                  "medioDeContacto": "email",
                  "direccionDeContacto": "juan@test.com",
                  "asuntoMensaje": "Asunto",
                  "cuerpoMensaje": "   "
                }
                """;
        Message mensaje = crearMensajeAmqp(jsonIncompleto, "notificaciones.donacion");

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumidor.recibir(mensaje));
        assertNull(servicio.ultimoCuerpo);
    }

    @Test
    @DisplayName("Punto 32: Mensaje corrupto que no es JSON válido se rechaza a DLQ sin tragarlo")
    void mensajeNoValidoSeRechazaADeadLetterSinTragar() {
        Message mensaje = crearMensajeAmqp("esto no es json {", "notificaciones.donacion");

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumidor.recibir(mensaje));
        assertNull(servicio.ultimoMedio);
    }

    @Test
    @DisplayName("Punto 32: Caída de base de datos durante solicitud se propaga para activar reintentos de RabbitMQ")
    void errorDeBaseDeDatosEnSolicitudSePropagaParaReintento() {
        String jsonValido = """
                {
                  "medioDeContacto": "email",
                  "direccionDeContacto": "juan@test.com",
                  "asuntoMensaje": "Asunto",
                  "cuerpoMensaje": "Cuerpo"
                }
                """;
        Message mensaje = crearMensajeAmqp(jsonValido, "notificaciones.donacion");
        servicio.lanzarErrorEnSolicitud = true;

        assertThrows(RuntimeException.class, () -> consumidor.recibir(mensaje));
    }

    @Test
    @DisplayName("Solicitud completa y válida se procesa y persiste correctamente")
    void solicitudValidaSeProcesaCorrectamente() {
        String jsonValido = """
                {
                  "medioDeContacto": "email",
                  "direccionDeContacto": "juan@test.com",
                  "asuntoMensaje": "Asunto de prueba",
                  "cuerpoMensaje": "Cuerpo de prueba"
                }
                """;
        Message mensaje = crearMensajeAmqp(jsonValido, "notificaciones.donacion");

        assertDoesNotThrow(() -> consumidor.recibir(mensaje));

        assertEquals("email", servicio.ultimoMedio);
        assertEquals("juan@test.com", servicio.ultimaDireccion);
        assertEquals("Asunto de prueba", servicio.ultimoAsunto);
        assertEquals("Cuerpo de prueba", servicio.ultimoCuerpo);
    }

    @Test
    @DisplayName("Aviso ya enviado no se vuelve a despachar")
    void avisoYaEnviadoNoSeVuelveAEnviar() {
        UUID id = UUID.randomUUID();
        String jsonAviso = String.format("""
                {
                  "id": "%s",
                  "medioDeContacto": "email",
                  "direccionDeContacto": "juan@test.com"
                }
                """, id);
        Message mensaje = crearMensajeAmqp(jsonAviso, ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig.RK_AVISO);

        Notificacion notificacion = new Notificacion("juan@test.com", "email", new Mensaje("A", "C"));
        notificacion.marcarEnviada();
        repositorio.tabla.put(id, notificacion);

        assertDoesNotThrow(() -> consumidor.recibir(mensaje));

        assertEquals(0, servicio.llamadasEnviarNotificacion);
    }

    @Test
    @DisplayName("Punto 33: Solicitud entrante que contiene un campo 'id' se procesa como solicitud y no se confunde con un aviso")
    void solicitudConCampoIdSeProcesaComoSolicitud() {
        String jsonSolicitudConId = """
                {
                  "id": "donacion-12345",
                  "medioDeContacto": "email",
                  "direccionDeContacto": "juan@test.com",
                  "asuntoMensaje": "Donación confirmada",
                  "cuerpoMensaje": "Gracias por su donación"
                }
                """;
        Message mensaje = crearMensajeAmqp(jsonSolicitudConId, "notificaciones.donacion");

        assertDoesNotThrow(() -> consumidor.recibir(mensaje));

        assertEquals("email", servicio.ultimoMedio);
        assertEquals("juan@test.com", servicio.ultimaDireccion);
        assertEquals("Donación confirmada", servicio.ultimoAsunto);
        assertEquals("Gracias por su donación", servicio.ultimoCuerpo);
    }
    @Test
    @DisplayName("Una solicitud con urlMapa de más de 1024 caracteres se rechaza a la dead letter")
    void solicitudConUrlMapaDemasiadoLargoSeRechazaADeadLetter() {
        String jsonSolicitud = String.format("""
                {
                  "medioDeContacto": "email",
                  "direccionDeContacto": "ana@test.com",
                  "asuntoMensaje": "Ruta",
                  "cuerpoMensaje": "En viaje",
                  "urlMapa": "%s"
                }
                """, "a".repeat(1025));
        Message mensaje = crearMensajeAmqp(jsonSolicitud, "notificaciones.donacion");

        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> consumidor.recibir(mensaje));
    }

    @Test
    @DisplayName("El urlMapa de una solicitud se propaga al gestor")
    void solicitudConUrlMapaLaPropaga() {
        String jsonSolicitud = """
                {
                  "medioDeContacto": "email",
                  "direccionDeContacto": "ana@test.com",
                  "asuntoMensaje": "Ruta",
                  "cuerpoMensaje": "En viaje",
                  "urlMapa": "https://mapa/1"
                }
                """;
        Message mensaje = crearMensajeAmqp(jsonSolicitud, "notificaciones.donacion");

        assertDoesNotThrow(() -> consumidor.recibir(mensaje));

        assertEquals("https://mapa/1", servicio.ultimaUrlMapa);
    }

    @Test
    @DisplayName("Aviso con notificación PENDIENTE se despacha exitosamente al medio")
    void avisoPendienteSeDespachaCorrectamente() {
        UUID id = UUID.randomUUID();
        String jsonAviso = String.format("""
                {
                  "id": "%s",
                  "medioDeContacto": "email",
                  "direccionDeContacto": "ana@test.com"
                }
                """, id);
        Message mensaje = crearMensajeAmqp(jsonAviso, ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig.RK_AVISO);

        Notificacion notificacion = new Notificacion("ana@test.com", "email", new Mensaje("Asunto", "Cuerpo"));
        repositorio.tabla.put(id, notificacion);

        assertDoesNotThrow(() -> consumidor.recibir(mensaje));

        assertEquals(1, servicio.llamadasEnviarNotificacion);
        assertEquals("email", servicio.ultimoMedio);
        assertEquals("ana@test.com", servicio.ultimaDireccion);
        assertEquals(notificacion, servicio.ultimaNotificacionDespachada);
    }

    @Test
    @DisplayName("Punto 44: el aviso se reclama en una transacción corta antes de salir a n8n")
    void avisoSeReclamaAntesDeEnviar() {
        UUID id = UUID.randomUUID();
        String jsonAviso = String.format("""
                {
                  "id": "%s",
                  "medioDeContacto": "email",
                  "direccionDeContacto": "ana@test.com"
                }
                """, id);
        Message mensaje = crearMensajeAmqp(jsonAviso, ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig.RK_AVISO);
        repositorio.tabla.put(id, new Notificacion("ana@test.com", "email", new Mensaje("Asunto", "Cuerpo")));

        assertDoesNotThrow(() -> consumidor.recibir(mensaje));

        assertEquals(1, servicio.llamadasReclamarEnvio);
        assertEquals(1, servicio.llamadasMarcarEnviada);
        assertEquals(EstadoNotificacion.ENVIANDO, servicio.estadoAlEnviar,
                "el claim tiene que estar commiteado antes de salir a n8n");
    }

    @Test
    @DisplayName("Punto 44: el listener no envuelve el HTTP en una transacción")
    void elListenerNoEsTransaccional() throws Exception {
        assertNull(ConsumidorNotificaciones.class.getMethod("recibir", Message.class)
                .getAnnotation(Transactional.class));
    }

    @Test
    @DisplayName("Punto 38: falla transitoria de n8n se propaga para reintento y deja la notificación FALLIDA")
    void avisoConFallaTransitoriaSePropagaParaReintento() {
        UUID id = UUID.randomUUID();
        String jsonAviso = String.format("""
                {
                  "id": "%s",
                  "medioDeContacto": "email",
                  "direccionDeContacto": "ana@test.com"
                }
                """, id);
        Message mensaje = crearMensajeAmqp(jsonAviso, ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig.RK_AVISO);

        Notificacion notificacion = new Notificacion("ana@test.com", "email", new Mensaje("Asunto", "Cuerpo"));
        repositorio.tabla.put(id, notificacion);
        servicio.errorEnviarNotificacion = new ResourceAccessException("timeout contra n8n");

        assertThrows(ResourceAccessException.class, () -> consumidor.recibir(mensaje));
        assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstado());
    }

    @Test
    @DisplayName("Punto 38: 4xx de n8n se rechaza directo a DLQ sin reintentar")
    void avisoCon4xxDeN8nSeRechazaADeadLetter() {
        UUID id = UUID.randomUUID();
        String jsonAviso = String.format("""
                {
                  "id": "%s",
                  "medioDeContacto": "email",
                  "direccionDeContacto": "ana@test.com"
                }
                """, id);
        Message mensaje = crearMensajeAmqp(jsonAviso, ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig.RK_AVISO);

        Notificacion notificacion = new Notificacion("ana@test.com", "email", new Mensaje("Asunto", "Cuerpo"));
        repositorio.tabla.put(id, notificacion);
        servicio.errorEnviarNotificacion = new HttpClientErrorException(HttpStatus.BAD_REQUEST);

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumidor.recibir(mensaje));
        assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstado());
    }

    @Test
    @DisplayName("Un 429 de n8n se propaga para reintento y deja la notificación FALLIDA")
    void avisoCon429SePropagaParaReintento() {
        UUID id = UUID.randomUUID();
        String jsonAviso = String.format("""
                {
                  "id": "%s",
                  "medioDeContacto": "email",
                  "direccionDeContacto": "ana@test.com"
                }
                """, id);
        Message mensaje = crearMensajeAmqp(jsonAviso, ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig.RK_AVISO);

        Notificacion notificacion = new Notificacion("ana@test.com", "email", new Mensaje("Asunto", "Cuerpo"));
        repositorio.tabla.put(id, notificacion);
        servicio.errorEnviarNotificacion = new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS);

        assertThrows(HttpClientErrorException.class, () -> consumidor.recibir(mensaje));
        assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstado());
    }

    @Test
    @DisplayName("Punto 38: medio de contacto inválido se rechaza directo a DLQ sin reintentar")
    void avisoConMedioInvalidoSeRechazaADeadLetter() {
        UUID id = UUID.randomUUID();
        String jsonAviso = String.format("""
                {
                  "id": "%s",
                  "medioDeContacto": "telegram",
                  "direccionDeContacto": "ana@test.com"
                }
                """, id);
        Message mensaje = crearMensajeAmqp(jsonAviso, ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig.RK_AVISO);

        Notificacion notificacion = new Notificacion("ana@test.com", "telegram", new Mensaje("Asunto", "Cuerpo"));
        repositorio.tabla.put(id, notificacion);
        servicio.errorEnviarNotificacion = new IllegalArgumentException("Tipo de medio de contacto desconocido: 'telegram'");

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumidor.recibir(mensaje));
        assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstado());
    }

    @Test
    @DisplayName("Punto 38: 5xx de n8n se propaga para reintento")
    void avisoCon5xxSePropagaParaReintento() {
        UUID id = UUID.randomUUID();
        String jsonAviso = String.format("""
                {
                  "id": "%s",
                  "medioDeContacto": "email",
                  "direccionDeContacto": "ana@test.com"
                }
                """, id);
        Message mensaje = crearMensajeAmqp(jsonAviso, ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig.RK_AVISO);

        Notificacion notificacion = new Notificacion("ana@test.com", "email", new Mensaje("Asunto", "Cuerpo"));
        repositorio.tabla.put(id, notificacion);
        servicio.errorEnviarNotificacion = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThrows(HttpServerErrorException.class, () -> consumidor.recibir(mensaje));
        assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstado());
    }

    @Test
    @DisplayName("Punto 38: una notificación FALLIDA se recupera si un reintento posterior tiene éxito")
    void notificacionFallidaSeRecuperaEnReintento() {
        UUID id = UUID.randomUUID();
        String jsonAviso = String.format("""
                {
                  "id": "%s",
                  "medioDeContacto": "email",
                  "direccionDeContacto": "ana@test.com"
                }
                """, id);
        Message mensaje = crearMensajeAmqp(jsonAviso, ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig.RK_AVISO);

        Notificacion notificacion = new Notificacion("ana@test.com", "email", new Mensaje("Asunto", "Cuerpo"));
        repositorio.tabla.put(id, notificacion);

        servicio.errorEnviarNotificacion = new ResourceAccessException("timeout contra n8n");
        assertThrows(ResourceAccessException.class, () -> consumidor.recibir(mensaje));
        assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstado());

        servicio.errorEnviarNotificacion = null;
        assertDoesNotThrow(() -> consumidor.recibir(mensaje));
        assertEquals(EstadoNotificacion.ENVIADA, notificacion.getEstado());
    }

    @Test
    @DisplayName("Aviso con id inexistente en BD se rechaza a DLQ")
    void avisoConIdNoExistenteSeRechazaADeadLetter() {
        UUID idInexistente = UUID.randomUUID();
        String jsonAviso = String.format("""
                {
                  "id": "%s",
                  "medioDeContacto": "email",
                  "direccionDeContacto": "nadie@test.com"
                }
                """, idInexistente);
        Message mensaje = crearMensajeAmqp(jsonAviso, ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig.RK_AVISO);

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumidor.recibir(mensaje));
        assertEquals(0, servicio.llamadasEnviarNotificacion);
    }

    @Test
    @DisplayName("Aviso con id con formato no UUID se rechaza a DLQ")
    void avisoConIdNoUUIDSeRechazaADeadLetter() {
        String jsonAviso = """
                {
                  "id": "formato-invalido-no-uuid",
                  "medioDeContacto": "email",
                  "direccionDeContacto": "ana@test.com"
                }
                """;
        Message mensaje = crearMensajeAmqp(jsonAviso, ar.edu.utn.frba.ddsi.notificaciones.config.rabbit.RabbitConfig.RK_AVISO);

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumidor.recibir(mensaje));
        assertEquals(0, servicio.llamadasEnviarNotificacion);
    }

    @Test
    @DisplayName("Solicitud sin medioDeContacto se rechaza a DLQ")
    void solicitudSinMedioDeContactoSeRechazaADeadLetter() {
        String jsonSinMedio = """
                {
                  "direccionDeContacto": "ana@test.com",
                  "asuntoMensaje": "Asunto",
                  "cuerpoMensaje": "Cuerpo"
                }
                """;
        Message mensaje = crearMensajeAmqp(jsonSinMedio, "notificaciones.donacion");

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumidor.recibir(mensaje));
        assertEquals(0, servicio.llamadasEnviarSolicitud);
    }

    @Test
    @DisplayName("Solicitud sin direccionDeContacto se rechaza a DLQ")
    void solicitudSinDireccionDeContactoSeRechazaADeadLetter() {
        String jsonSinDireccion = """
                {
                  "medioDeContacto": "email",
                  "direccionDeContacto": "   ",
                  "asuntoMensaje": "Asunto",
                  "cuerpoMensaje": "Cuerpo"
                }
                """;
        Message mensaje = crearMensajeAmqp(jsonSinDireccion, "notificaciones.donacion");

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumidor.recibir(mensaje));
        assertEquals(0, servicio.llamadasEnviarSolicitud);
    }
}
