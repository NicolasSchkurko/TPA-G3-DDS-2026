package ar.edu.utn.frba.ddsi.front.services;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.NotificacionesServicioClient;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.front.dto.NotificacionServicioDTO;
import ar.edu.utn.frba.ddsi.front.dto.SeguimientoDTO;
import ar.edu.utn.frba.ddsi.front.dto.logistica.LogisticaDTOs;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;

/**
 * Caché por request de los datos de la entidad actual. Evita llamadas duplicadas al backend
 * cuando el header (contador de avisos) y la página necesitan los mismos datos.
 */
@Component
public class EntidadContext {

    private final IdentidadService identidadService;
    private final DonacionesClient donacionesClient;
    private final SeguimientoService seguimientoService;
    private final NotificacionesServicioClient notificacionesServicioClient;

    private static final DateTimeFormatter FECHA_SERVICIO = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", new Locale("es", "AR"));

    public EntidadContext(IdentidadService identidadService,
                          DonacionesClient donacionesClient,
                          SeguimientoService seguimientoService,
                          NotificacionesServicioClient notificacionesServicioClient) {
        this.identidadService = identidadService;
        this.donacionesClient = donacionesClient;
        this.seguimientoService = seguimientoService;
        this.notificacionesServicioClient = notificacionesServicioClient;
    }

    public UUID entidadId(HttpServletRequest request, Authentication authentication) {
        Object cached = request.getAttribute("entidad.id");
        if (cached instanceof UUID id) return id;
        UUID id = identidadService.entidadIdActual(authentication).orElse(null);
        request.setAttribute("entidad.id", id);
        return id;
    }

    /** Entidad actual; null si no hay vínculo o el backend no responde. */
    public EntidadBeneficiariaDTO entidad(HttpServletRequest request, Authentication authentication) {
        Object cached = request.getAttribute("entidad.dto");
        if (cached != null) return (EntidadBeneficiariaDTO) cached;
        UUID id = entidadId(request, authentication);
        EntidadBeneficiariaDTO entidad = id != null ? donacionesClient.obtenerEntidadPorId(id) : null;
        if (entidad != null) {
            entidad.setNecesidades(donacionesClient.obtenerNecesidadesDeEntidad(id));
        }
        request.setAttribute("entidad.dto", entidad);
        return entidad;
    }

    public List<DonacionDTO> donaciones(HttpServletRequest request, Authentication authentication) {
        Object cached = request.getAttribute("entidad.donaciones");
        if (cached != null) return castList(cached);
        UUID id = entidadId(request, authentication);
        List<DonacionDTO> donaciones = id != null ? donacionesClient.obtenerDonacionesDeEntidad(id) : List.of();
        request.setAttribute("entidad.donaciones", donaciones);
        return donaciones;
    }

    public Map<UUID, LogisticaDTOs.Bien> entregasPorDonacion(HttpServletRequest request) {
        Object cached = request.getAttribute("entidad.entregas");
        if (cached != null) return castMap(cached);
        Map<UUID, LogisticaDTOs.Bien> entregas = seguimientoService.entregasPorDonacion();
        request.setAttribute("entidad.entregas", entregas);
        return entregas;
    }

    public SeguimientoDTO seguimiento(HttpServletRequest request, Authentication authentication) {
        Object cached = request.getAttribute("entidad.seguimiento");
        if (cached != null) return (SeguimientoDTO) cached;
        UUID id = entidadId(request, authentication);
        SeguimientoDTO seguimiento = new SeguimientoDTO();
        seguimiento.setGeneradoEn(java.time.OffsetDateTime.now().toString());
        if (id != null) {
            seguimiento = seguimientoService.construir(id, donaciones(request, authentication));
        }
        request.setAttribute("entidad.seguimiento", seguimiento);
        return seguimiento;
    }

    /** Notificaciones enviadas por notificaciones-service al email de la entidad (sistema). */
    public List<NotificacionesDerivadasService.Notificacion> notificacionesServicio(HttpServletRequest request,
                                                                                    Authentication authentication,
                                                                                    HttpSession session) {
        Object cached = request.getAttribute("entidad.notifServicio");
        if (cached != null) return castNotificaciones(cached);
        List<NotificacionesDerivadasService.Notificacion> resultado = construirNotificacionesServicio(request, authentication, session);
        request.setAttribute("entidad.notifServicio", resultado);
        return resultado;
    }

    /** Actividad derivada del estado de donaciones y entregas (sin las notificaciones del sistema). */
    public List<NotificacionesDerivadasService.Notificacion> actividad(HttpServletRequest request,
                                                                       Authentication authentication,
                                                                       HttpSession session) {
        Object cached = request.getAttribute("entidad.actividad");
        if (cached != null) return castNotificaciones(cached);
        List<NotificacionesDerivadasService.Notificacion> resultado =
                NotificacionesDerivadasService.derivar(donaciones(request, authentication),
                        entregasPorDonacion(request), leidasEnSesion(session));
        request.setAttribute("entidad.actividad", resultado);
        return resultado;
    }

    /** Bandeja completa: notificaciones del sistema + actividad derivada. */
    public List<NotificacionesDerivadasService.Notificacion> notificaciones(HttpServletRequest request,
                                                                            Authentication authentication,
                                                                            HttpSession session) {
        Object cached = request.getAttribute("entidad.notificaciones");
        if (cached != null) return castNotificaciones(cached);
        List<NotificacionesDerivadasService.Notificacion> notificaciones = new ArrayList<>();
        notificaciones.addAll(notificacionesServicio(request, authentication, session));
        notificaciones.addAll(actividad(request, authentication, session));
        request.setAttribute("entidad.notificaciones", notificaciones);
        return notificaciones;
    }

    public Set<String> leidasEnSesion(HttpSession session) {
        Object value = session.getAttribute("entidad.notifLeidas");
        if (value instanceof Set<?> set) {
            @SuppressWarnings("unchecked")
            Set<String> leidas = (Set<String>) set;
            return leidas;
        }
        Set<String> leidas = new HashSet<>();
        session.setAttribute("entidad.notifLeidas", leidas);
        return leidas;
    }

    private List<NotificacionesDerivadasService.Notificacion> construirNotificacionesServicio(
            HttpServletRequest request, Authentication authentication, HttpSession session) {
        EntidadBeneficiariaDTO entidad = entidad(request, authentication);
        String email = entidad != null ? normalizarEmail(entidad.getEmail()) : null;
        if (email == null) return List.of();

        Set<String> leidas = leidasEnSesion(session);
        List<NotificacionServicioDTO> dtos = notificacionesServicioClient.listarPorDestinatario(email, 20);
        List<NotificacionesDerivadasService.Notificacion> resultado = new ArrayList<>();
        for (NotificacionServicioDTO dto : dtos) {
            if (dto.getId() == null) continue;
            String id = "svc-" + dto.getId();
            String asunto = dto.getAsunto() != null && !dto.getAsunto().isBlank() ? dto.getAsunto() : "Notificación";
            String cuerpo = dto.getCuerpo() != null ? dto.getCuerpo() : "";
            resultado.add(new NotificacionesDerivadasService.Notificacion(
                    id,
                    "servicio",
                    asunto,
                    cuerpo,
                    formatoFechaServicio(dto.getFechaCreacion()),
                    null,
                    leidas.contains(id),
                    "sistema",
                    metaServicio(dto)));
        }
        return resultado;
    }

    private static String normalizarEmail(String email) {
        if (email == null) return null;
        String normalizado = email.trim().toLowerCase();
        return normalizado.isEmpty() ? null : normalizado;
    }

    private static String formatoFechaServicio(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.isBlank()) return null;
        try {
            return LocalDateTime.parse(isoDateTime).format(FECHA_SERVICIO);
        } catch (Exception e) {
            return isoDateTime;
        }
    }

    private static String metaServicio(NotificacionServicioDTO dto) {
        List<String> partes = new ArrayList<>();
        if (dto.getTipoMedioDeContacto() != null && !dto.getTipoMedioDeContacto().isBlank()) {
            partes.add(etiquetaMedio(dto.getTipoMedioDeContacto()));
        }
        if (dto.getEstado() != null && !dto.getEstado().isBlank()) {
            partes.add(dto.getEstado().trim().toUpperCase(Locale.ROOT));
        }
        return partes.isEmpty() ? null : String.join(" · ", partes);
    }

    private static String etiquetaMedio(String tipoMedio) {
        return switch (tipoMedio.trim().toUpperCase(Locale.ROOT)) {
            case "EMAIL" -> "Correo";
            case "TELEFONO" -> "Teléfono";
            case "WHATSAPP" -> "WhatsApp";
            case "SMS" -> "SMS";
            case "PUSH" -> "Push";
            default -> tipoMedio;
        };
    }

    @SuppressWarnings("unchecked")
    private static List<DonacionDTO> castList(Object cached) {
        return (List<DonacionDTO>) cached;
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, LogisticaDTOs.Bien> castMap(Object cached) {
        return (Map<UUID, LogisticaDTOs.Bien>) cached;
    }

    @SuppressWarnings("unchecked")
    private static List<NotificacionesDerivadasService.Notificacion> castNotificaciones(Object cached) {
        return (List<NotificacionesDerivadasService.Notificacion>) cached;
    }
}
