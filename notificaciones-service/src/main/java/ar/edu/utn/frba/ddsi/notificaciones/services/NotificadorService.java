package ar.edu.utn.frba.ddsi.notificaciones.services;

import ar.edu.utn.frba.ddsi.notificaciones.dto.Notificaciones.NotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.dto.Notificaciones.SolicitudNotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.mappers.NotificacionMapper;
import ar.edu.utn.frba.ddsi.notificaciones.messaging.ProductorNotificaciones;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.gestores.GestorEnvio;
import ar.edu.utn.frba.ddsi.notificaciones.models.gestores.GestorSolicitudes;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories.RepositorioNotificaciones;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificadorService {

    private static final Logger log = LoggerFactory.getLogger(NotificadorService.class);

    private final RepositorioNotificaciones repositorioNotificaciones;
    private final GestorSolicitudes gestorSolicitudes;
    private final GestorEnvio gestorEnvio;
    private final ProductorNotificaciones productorNotificaciones;
    private final NotificacionMapper notificacionMapper;

    public NotificadorService(RepositorioNotificaciones repositorioNotificaciones,
                              GestorSolicitudes gestorSolicitudes,
                              GestorEnvio gestorEnvio,
                              ProductorNotificaciones productorNotificaciones,
                              NotificacionMapper notificacionMapper) {
        this.repositorioNotificaciones = repositorioNotificaciones;
        this.gestorSolicitudes = gestorSolicitudes;
        this.gestorEnvio = gestorEnvio;
        this.productorNotificaciones = productorNotificaciones;
        this.notificacionMapper = notificacionMapper;
    }

    @Transactional
    public UUID procesarSolicitudDeNotificacion(SolicitudNotificacionDTO dto) {
        gestorSolicitudes.validar(dto);
        return gestorSolicitudes
                .idSiYaExiste(repositorioNotificaciones.findByIdSolicitud(dto.getIdSolicitud()))
                .orElseGet(() -> crearYPublicar(dto));
    }

    private UUID crearYPublicar(SolicitudNotificacionDTO dto) {
        Notificacion notificacion = Notificacion.crear(
                dto.getIdSolicitud(),
                dto.getMedioDeContacto(),
                dto.getDireccionDeContacto(),
                dto.getAsuntoMensaje(),
                dto.getCuerpoMensaje(),
                dto.getUrlMapa());
        repositorioNotificaciones.save(notificacion);

        log.info("[SERVICE] Notificación {} guardada como {} (medio='{}', destino='{}', idSolicitud='{}')",
                notificacion.getId(), notificacion.getEstado(), dto.getMedioDeContacto(),
                dto.getDireccionDeContacto(), dto.getIdSolicitud());

        publicarAlCommit(notificacion);
        return notificacion.getId();
    }

    private void publicarAlCommit(Notificacion notificacion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("[SERVICE] Transacción confirmada: publicando el aviso de la notificación {}",
                            notificacion.getId());
                    productorNotificaciones.enviar(notificacion);
                }
            });
        } else {
            productorNotificaciones.enviar(notificacion);
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificacionDTO> listar(EstadoNotificacion estado, Pageable pageable) {
        return repositorioNotificaciones.buscar(estado, pageable)
                .map(notificacionMapper::notificacionDTO);
    }

    @Transactional(readOnly = true)
    public Page<NotificacionDTO> listarPorDestinatario(String destinatario,
                                                       EstadoNotificacion estado,
                                                       Pageable pageable) {
        return repositorioNotificaciones.buscarPorDestinatario(destinatario, estado, pageable)
                .map(notificacionMapper::notificacionDTO);
    }

    public Optional<Notificacion> obtenerPorId(UUID id) {
        return repositorioNotificaciones.findById(id);
    }

    /** Republica el aviso de una notificación no enviada; 404 si no existe y 409 si ya salió. */
    @Transactional
    public void reintentar(UUID id) {
        Notificacion notificacion = repositorioNotificaciones.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Notificación no encontrada: " + id));
        gestorSolicitudes.validarReintento(notificacion);
        productorNotificaciones.enviar(notificacion);
    }

    /**
     * Reclama el envío de un aviso en una transacción corta: toma el lock de la fila, la pasa a
     * ENVIANDO y commitea. La llamada HTTP a n8n queda fuera de la transacción.
     */
    @Transactional
    public Optional<Notificacion> reclamarParaEnvio(UUID id, String medioDeContacto,
                                                    String direccionDeContacto) {
        return gestorEnvio.reclamar(
                repositorioNotificaciones.findByIdParaEnvio(id),
                medioDeContacto,
                direccionDeContacto);
    }

    /** Delegado al gestor de envío; corre fuera de la transacción del claim. */
    public void enviarNotificacion(Notificacion notificacion,
                                   String tipoMedioDeContacto,
                                   String direccionDeContacto) {
        gestorEnvio.enviarNotificacion(tipoMedioDeContacto, direccionDeContacto, notificacion);
    }

    /** Cierra el envío en otra transacción corta; solo si el claim sigue siendo el mismo. */
    @Transactional
    public boolean marcarEnviada(UUID id, UUID claimId) {
        return repositorioNotificaciones.marcarEnviadaSiEnviando(
                id, claimId, LocalDateTime.now(), EstadoNotificacion.ENVIADA,
                EstadoNotificacion.ENVIANDO) > 0;
    }

    @Transactional
    public boolean marcarFallida(UUID id, UUID claimId) {
        return repositorioNotificaciones.marcarFallidaSiEnviando(
                id, claimId, EstadoNotificacion.FALLIDA, EstadoNotificacion.ENVIANDO) > 0;
    }

}
