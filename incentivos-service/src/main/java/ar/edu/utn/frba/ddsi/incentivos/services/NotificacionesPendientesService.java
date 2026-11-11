package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.PerfilNotificacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Notificaciones.NotificacionPendiente;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioNotificacionesPendientes;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Operaciones transaccionales de la outbox durable de notificaciones. */
@Slf4j
@Service
public class NotificacionesPendientesService {

    private static final long LEASE_SEGUNDOS = 60;
    private static final long RETRASO_BASE_SEGUNDOS = 30;
    private static final long RETRASO_MAXIMO_SEGUNDOS = 3600;
    private static final int MAX_INTENTOS = 10;

    private final RepositorioNotificacionesPendientes repositorio;

    public NotificacionesPendientesService(RepositorioNotificacionesPendientes repositorio) {
        this.repositorio = repositorio;
    }

    /** Persiste una notificación que no se pudo publicar. */
    @Transactional
    public void encolar(PerfilNotificacionDTO dto) {
        repositorio.save(new NotificacionPendiente(dto, LocalDateTime.now()));
    }

    /** Reclama una notificación disponible y confirma el lease antes de publicarla. */
    @Transactional
    public Optional<NotificacionReclamada> reclamarSiguiente(LocalDateTime ahora) {
        return repositorio.buscarSiguienteParaEnviar(ahora, PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(pendiente -> {
                    pendiente.reclamarHasta(ahora.plusSeconds(LEASE_SEGUNDOS));
                    return new NotificacionReclamada(
                            pendiente.getId(), pendiente.comoDto(), pendiente.getIntentos());
                });
    }

    /** Elimina una notificación que el broker aceptó. */
    @Transactional
    public void confirmar(UUID id) {
        repositorio.deleteById(id);
    }

    /** Libera el lease y agenda el siguiente intento con espera exponencial acotada. */
    @Transactional
    public void reintentar(UUID id, LocalDateTime ahora, String error) {
        repositorio.findById(id).ifPresent(pendiente -> {
            if (pendiente.getIntentos() >= MAX_INTENTOS) {
                pendiente.descartar(error);
                log.error("Notificación {} descartada tras {} intentos: {}",
                        id, pendiente.getIntentos(), error);
                return;
            }
            int exponente = Math.min(Math.max(pendiente.getIntentos() - 1, 0), 7);
            long espera = Math.min(RETRASO_BASE_SEGUNDOS * (1L << exponente),
                    RETRASO_MAXIMO_SEGUNDOS);
            pendiente.reintentarEn(ahora.plusSeconds(espera), error);
        });
    }

    /** Una notificación reclamada y lista para publicar fuera de la transacción. */
    public record NotificacionReclamada(UUID id,
                                        PerfilNotificacionDTO contenido,
                                        int intentos) {
    }
}
