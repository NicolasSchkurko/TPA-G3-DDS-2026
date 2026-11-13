package ar.edu.utn.frba.ddsi.front.services;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.NotificacionesServicioClient;
import ar.edu.utn.frba.ddsi.front.dto.AdminDTO;
import ar.edu.utn.frba.ddsi.front.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.front.dto.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.front.dto.NotificacionServicioDTO;
import ar.edu.utn.frba.ddsi.front.dto.PersonaDonanteDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bandeja genérica de notificaciones del sistema para cualquier rol.
 *
 * <p>Busca los avisos enviados por notificaciones-service a todos los medios de contacto
 * asociados al usuario: el correo de la cuenta, los medios del perfil de donante
 * (email, teléfono, WhatsApp) y los datos de contacto del perfil de entidad.
 * El estado leído/no leído se persiste por usuario en notificaciones-service.
 */
@Service
public class NotificacionesUsuarioService {

    private static final String REQUEST_CACHE = "notifUsuario.notificaciones";
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", new Locale("es", "AR"));

    private final NotificacionesServicioClient notificacionesServicioClient;
    private final DonacionesClient donacionesClient;
    private final IdentidadService identidadService;

    public NotificacionesUsuarioService(NotificacionesServicioClient notificacionesServicioClient,
                                        DonacionesClient donacionesClient,
                                        IdentidadService identidadService) {
        this.notificacionesServicioClient = notificacionesServicioClient;
        this.donacionesClient = donacionesClient;
        this.identidadService = identidadService;
    }

    /** Notificaciones del sistema de todos los medios de contacto del usuario, más recientes primero. */
    public List<NotificacionesDerivadasService.Notificacion> notificaciones(HttpServletRequest request,
                                                                            Authentication authentication) {
        Object cached = request.getAttribute(REQUEST_CACHE);
        if (cached instanceof List<?> lista) {
            @SuppressWarnings("unchecked")
            List<NotificacionesDerivadasService.Notificacion> cast =
                    (List<NotificacionesDerivadasService.Notificacion>) lista;
            return cast;
        }

        List<NotificacionesDerivadasService.Notificacion> resultado = construir(authentication);
        request.setAttribute(REQUEST_CACHE, resultado);
        return resultado;
    }

    /** Cantidad de avisos sin leer. */
    public long sinLeer(HttpServletRequest request, Authentication authentication) {
        return NotificacionesDerivadasService.sinLeer(notificaciones(request, authentication));
    }

    /** Marca todos los avisos visibles como leídos de forma persistente. */
    public void marcarTodasLeidas(HttpServletRequest request, Authentication authentication) {
        String usuarioId = usuarioId(authentication);
        Set<String> ids = idsServicio(notificaciones(request, authentication));
        notificacionesServicioClient.marcarLeidas(usuarioId, ids);
        request.removeAttribute(REQUEST_CACHE);
    }

    /** Marca un aviso puntual como leído de forma persistente. */
    public void marcarLeida(HttpServletRequest request, Authentication authentication, String id) {
        if (id == null || id.isBlank()) return;
        String idNormalizado = id.trim();
        boolean perteneceAlUsuario = notificaciones(request, authentication).stream()
                .anyMatch(notificacion -> notificacion.id().equals(idNormalizado));
        if (!perteneceAlUsuario) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        notificacionesServicioClient.marcarLeidas(usuarioId(authentication),
                Set.of(idNormalizado.substring("svc-".length())));
        request.removeAttribute(REQUEST_CACHE);
    }

    private List<NotificacionesDerivadasService.Notificacion> construir(Authentication authentication) {
        Set<String> destinatarios = destinatarios(authentication);
        if (destinatarios.isEmpty()) return List.of();

        Set<String> leidas = notificacionesServicioClient.listarLeidas(usuarioId(authentication)).stream()
                .map(id -> "svc-" + id)
                .collect(Collectors.toSet());
        Map<String, NotificacionServicioDTO> unicas = new LinkedHashMap<>();
        for (String destinatario : destinatarios) {
            for (NotificacionServicioDTO dto : notificacionesServicioClient.listarPorDestinatario(destinatario, 20)) {
                if (dto == null || dto.getId() == null) continue;
                unicas.putIfAbsent(dto.getId(), dto);
            }
        }

        List<Ordenada> ordenadas = new ArrayList<>();
        for (NotificacionServicioDTO dto : unicas.values()) {
            String id = "svc-" + dto.getId();
            String asunto = noVacio(dto.getAsunto(), "Notificación");
            String cuerpo = noVacio(dto.getCuerpo(), "");
            NotificacionesDerivadasService.Notificacion notificacion =
                    new NotificacionesDerivadasService.Notificacion(
                            id,
                            "servicio",
                            asunto,
                            cuerpo,
                            formatoFecha(dto.getFechaCreacion()),
                            null,
                            leidas.contains(id),
                            "sistema",
                            meta(dto));
            ordenadas.add(new Ordenada(parseFecha(dto.getFechaCreacion()), notificacion));
        }

        return ordenadas.stream()
                .sorted(Comparator.comparing((Ordenada item) -> item.fecha(),
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(Ordenada::notificacion)
                .toList();
    }

    private static Set<String> idsServicio(List<NotificacionesDerivadasService.Notificacion> notificaciones) {
        return notificaciones.stream()
                .map(NotificacionesDerivadasService.Notificacion::id)
                .filter(id -> id.startsWith("svc-"))
                .map(id -> id.substring("svc-".length()))
                .collect(Collectors.toSet());
    }

    /** Todos los destinatarios conocidos del usuario: correo de cuenta + medios de donante y entidad. */
    private Set<String> destinatarios(Authentication authentication) {
        Set<String> resultado = new LinkedHashSet<>();
        String correo = emailDe(authentication);
        agregar(resultado, correo);

        if (correo != null) {
            for (PersonaDonanteDTO donante : donacionesClient.buscarDonantesPorCorreo(correo)) {
                if (donante.getMediosDeContacto() != null) {
                    for (MediosContactoDTO contacto : donante.getMediosDeContacto()) {
                        if (contacto != null) agregar(resultado, contacto.getValor());
                    }
                }
                agregar(resultado, donante.getTelefono());
            }
        }

        Optional<EntidadBeneficiariaDTO> entidad = identidadService.entidadActual(authentication);
        entidad.ifPresent(dto -> {
            agregar(resultado, dto.getEmail());
            agregar(resultado, dto.getCorreoElectronico());
            agregar(resultado, dto.getTelefono());
        });

        identidadService.adminIdActual(authentication).ifPresent(idAdmin -> {
            AdminDTO admin = donacionesClient.obtenerAdminPorId(idAdmin);
            if (admin == null) return;
            agregar(resultado, admin.getEmail());
            if (admin.getMedioDeContacto() != null) {
                agregar(resultado, admin.getMedioDeContacto().getValor());
            }
        });

        return resultado;
    }

    private static void agregar(Set<String> destinatarios, String valor) {
        if (valor == null) return;
        String normalizado = valor.trim().toLowerCase(Locale.ROOT);
        if (normalizado.isBlank()) return;
        destinatarios.add(normalizado);
        if (!normalizado.contains("@")) {
            String digitos = normalizado.replaceAll("[^0-9]", "");
            if (digitos.length() >= 6) {
                destinatarios.add(digitos);
            }
        }
    }

    private static String noVacio(String valor, String porDefecto) {
        return valor != null && !valor.isBlank() ? valor.trim() : porDefecto;
    }

    private static String meta(NotificacionServicioDTO dto) {
        String medio = noVacio(dto.getTipoMedioDeContacto(), "Aviso").toUpperCase(Locale.ROOT);
        String estado = noVacio(dto.getEstado(), "").toUpperCase(Locale.ROOT);
        return estado.isBlank() ? etiquetaMedio(medio) : etiquetaMedio(medio) + " · " + estado;
    }

    private static String etiquetaMedio(String medio) {
        return switch (medio) {
            case "EMAIL", "MAIL", "CORREO" -> "Correo";
            case "TELEFONO", "TEL", "CELULAR" -> "Teléfono";
            case "WHATSAPP", "WA" -> "WhatsApp";
            default -> medio;
        };
    }

    private static String formatoFecha(String iso) {
        LocalDateTime fecha = parseFecha(iso);
        return fecha == null ? null : fecha.format(FECHA_HORA);
    }

    private static LocalDateTime parseFecha(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return LocalDateTime.parse(iso);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String emailDe(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String email = oidcUser.getEmail();
            if (email != null && !email.isBlank()) {
                return email.trim().toLowerCase(Locale.ROOT);
            }
        }
        return null;
    }

    private static String usuarioId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser
                && oidcUser.getSubject() != null && !oidcUser.getSubject().isBlank()) {
            return oidcUser.getSubject().trim();
        }
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return "authentication:" + authentication.getName().trim().toLowerCase(Locale.ROOT);
        }
        throw new IllegalStateException("No se pudo identificar al usuario autenticado para persistir lecturas");
    }

    private record Ordenada(LocalDateTime fecha, NotificacionesDerivadasService.Notificacion notificacion) {}
}
