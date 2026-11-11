package ar.edu.utn.frba.ddsi.notificaciones.services;

import ar.edu.utn.frba.ddsi.notificaciones.dto.SolicitudNotificacionDTO;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.gestores.GestorNotificaciones;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class NotificadorService {

    private final GestorNotificaciones gestorNotificaciones;

    public NotificadorService(GestorNotificaciones gestorNotificaciones) {
        this.gestorNotificaciones = gestorNotificaciones;
    }

    public Notificacion procesarSolicitudDeNotificacion(SolicitudNotificacionDTO solicitudNotificacionDTO) {
        return gestorNotificaciones.guardarYEnviarSolicitud(
                solicitudNotificacionDTO.getIdSolicitud(),
                solicitudNotificacionDTO.getMedioDeContacto(),
                solicitudNotificacionDTO.getDireccionDeContacto(),
                solicitudNotificacionDTO.getAsuntoMensaje(),
                solicitudNotificacionDTO.getCuerpoMensaje());
    }

    public Optional<Notificacion> obtenerPorId(UUID id) {
        return gestorNotificaciones.obtenerNotificacionPorId(id);
    }

    /**
     * Lista las notificaciones de un destinatario, más recientes primero.
     *
     * @param estado  PENDIENTE, ENVIADA o FALLIDA (insensible a mayúsculas); null/blank para todas.
     * @param page    página basada en cero (negativos se tratan como 0)
     * @param size    tamaño de página (se acota entre 1 y 50)
     */
    public java.util.List<Notificacion> listarPorDestinatario(String destinatario, String estado, int page, int size) {
        if (destinatario == null || destinatario.isBlank()) {
            throw new IllegalArgumentException("El parámetro 'destinatario' es obligatorio.");
        }
        EstadoNotificacion filtro = estadoNotificacion(estado);
        int tamaño = Math.min(Math.max(size, 1), 50);
        int pagina = Math.max(page, 0);
        Pageable pageable = PageRequest.of(pagina, tamaño, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        return gestorNotificaciones.listarPorDestinatario(destinatario.trim(), filtro, pageable).getContent();
    }

    private static EstadoNotificacion estadoNotificacion(String estado) {
        if (estado == null || estado.isBlank()) return null;
        try {
            return EstadoNotificacion.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "El estado '" + estado + "' no es válido. Valores admitidos: PENDIENTE, ENVIADA, FALLIDA.");
        }
    }
}
