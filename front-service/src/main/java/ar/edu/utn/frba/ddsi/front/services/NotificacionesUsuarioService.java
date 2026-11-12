package ar.edu.utn.frba.ddsi.front.services;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.NotificacionesServicioClient;
import ar.edu.utn.frba.ddsi.front.dto.AdminDTO;
import ar.edu.utn.frba.ddsi.front.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.front.dto.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.front.dto.NotificacionServicioDTO;
import ar.edu.utn.frba.ddsi.front.dto.PersonaDonanteDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Bandeja genérica de notificaciones del sistema para cualquier rol.
 *
 * <p>Busca los avisos enviados por notificaciones-service a todos los medios de contacto
 * asociados al usuario: el correo de la cuenta, los medios del perfil de donante
 * (email, teléfono, WhatsApp) y los datos de contacto del perfil de entidad.
 * El estado leído/no leído se guarda en la sesión HTTP (no persistente).
 */
@Service
public class NotificacionesUsuarioService {

    private static final String SESSION_LEIDAS = "notifUsuarioLeidas";
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
                                                                            Authentication authentication,
                                                                            HttpSession session) {
        Object cached = request.getAttribute(REQUEST_CACHE);
        if (cached instanceof List<?> lista) {
            @SuppressWarnings("unchecked")
            List<NotificacionesDerivadasService.Notificacion> cast =
                    (List<NotificacionesDerivadasService.Notificacion>) lista;
            return cast;
        }

        List<NotificacionesDerivadasService.Notificacion> resultado = construir(authentication, session);
        request.setAttribute(REQUEST_CACHE, resultado);
        return resultado;
    }

    /** Cantidad de avisos sin leer. */
    public long sinLeer(HttpServletRequest request, Authentication authentication, HttpSession session) {
        return NotificacionesDerivadasService.sinLeer(notificaciones(request, authentication, session));
    }

    /** Marca todos los avisos visibles como leídos en la sesión. */
    public void marcarTodasLeidas(HttpServletRequest request, Authentication authentication, HttpSession session) {
        Set<String> leidas = leidasEnSesion(session);
        for (NotificacionesDerivadasService.Notificacion notificacion : notificaciones(request, authentication, session)) {
            leidas.add(notificacion.id());
        }
        session.setAttribute(SESSION_LEIDAS, leidas);
        request.removeAttribute(REQUEST_CACHE);
    }

    /** Marca un aviso puntual como leído en la sesión. */
    public void marcarLeida(HttpServletRequest request, HttpSession session, String id) {
        if (id == null || id.isBlank()) return;
        Set<String> leidas = leidasEnSesion(session);
        leidas.add(id.trim());
        session.setAttribute(SESSION_LEIDAS, leidas);
        request.removeAttribute(REQUEST_CACHE);
    }

    private List<NotificacionesDerivadasService.Notificacion> construir(Authentication authentication, HttpSession session) {
        Set<String> destinatarios = destinatarios(authentication);
        if (destinatarios.isEmpty()) return List.of();

        Set<String> leidas = leidasEnSesion(session);
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

    private Set<String> leidasEnSesion(HttpSession session) {
        if (session == null) return new HashSet<>();
        Object value = session.getAttribute(SESSION_LEIDAS);
        if (value instanceof Set<?> set) {
            Set<String> leidas = new HashSet<>();
            for (Object item : set) {
                if (item instanceof String s) leidas.add(s);
            }
            return leidas;
        }
        Set<String> leidas = new HashSet<>();
        session.setAttribute(SESSION_LEIDAS, leidas);
        return leidas;
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

    private record Ordenada(LocalDateTime fecha, NotificacionesDerivadasService.Notificacion notificacion) {}
}
