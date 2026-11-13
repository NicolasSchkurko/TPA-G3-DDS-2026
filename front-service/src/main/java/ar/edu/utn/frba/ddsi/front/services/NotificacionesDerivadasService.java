package ar.edu.utn.frba.ddsi.front.services;

import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.logistica.LogisticaDTOs;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;

/**
 * Bandeja de avisos de la entidad: combina la actividad derivada del estado de las
 * donaciones y entregas con las notificaciones reales enviadas por notificaciones-service.
 * El estado leído/no leído se guarda en la sesión HTTP del usuario (no persistente).
 */
public final class NotificacionesDerivadasService {

    /**
     * Aviso de la bandeja.
     * {@code origen}: "actividad" (derivado del estado de donaciones/entregas) o
     * "sistema" (enviado por notificaciones-service). {@code meta} resume medio y estado.
     */
    public record Notificacion(String id, String tipo, String titulo, String detalle,
                               String fecha, String idDonacion, boolean leida,
                               String origen, String meta) {}

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d MMM yyyy", new Locale("es", "AR"));
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", new Locale("es", "AR"));

    private NotificacionesDerivadasService() {
    }

    /** Construye la bandeja de la entidad. {@code leidas} son los ids marcados como leídos en la sesión. */
    public static List<Notificacion> derivar(List<DonacionDTO> donaciones,
                                             Map<UUID, LogisticaDTOs.Bien> entregas,
                                             Set<String> leidas) {
        List<Notificacion> resultado = new ArrayList<>();
        if (donaciones == null) donaciones = List.of();

        for (DonacionDTO donacion : donaciones) {
            if (donacion.getId() == null) continue;
            String donacionId = donacion.getId().toString();
            String descripcion = donacion.getDescripcion() != null ? donacion.getDescripcion() : "Donación";
            String estado = donacion.getEstado() != null ? donacion.getEstado() : "";

            if (esAsignada(estado)) {
                resultado.add(new Notificacion("asignada-" + donacionId, "asignada",
                        "Donación asignada a tu entidad",
                        descripcion + " te fue asignada. Coordiná la recepción cuando llegue.",
                        format(donacion.getFechaEntrega()), donacionId, leidas.contains("asignada-" + donacionId),
                        "actividad", null));
            }

            LogisticaDTOs.Bien entrega = entregas.get(donacion.getId());
            if (entrega == null || entrega.getEstado() == null) continue;

            switch (entrega.getEstado().toUpperCase()) {
                case "EN_CAMINO" -> resultado.add(new Notificacion("camino-" + donacionId, "en_camino",
                        "Donación en camino",
                        "El camión va en camino con " + descripcion + ". Revisá el mapa de seguimiento.",
                        format(entrega.getFechaCambioEstado()), donacionId, leidas.contains("camino-" + donacionId),
                        "actividad", null));
                case "ENTREGADA" -> resultado.add(new Notificacion("recepcion-" + donacionId, "recepcion",
                        "Recepción confirmada",
                        "Confirmaste la recepción de " + descripcion + ". ¡Gracias!",
                        format(entrega.getFechaCambioEstado()), donacionId, leidas.contains("recepcion-" + donacionId),
                        "actividad", null));
                case "NO_RECIBIDA" -> resultado.add(new Notificacion("no-recibida-" + donacionId, "alerta",
                        "Entrega no recibida",
                        "La entrega de " + descripcion + " fue marcada como no recibida. Revisá el detalle.",
                        format(entrega.getFechaCambioEstado()), donacionId, leidas.contains("no-recibida-" + donacionId),
                        "actividad", null));
                default -> { }
            }
        }

        resultado.sort(Comparator.comparing(Notificacion::fecha, Comparator.nullsLast(Comparator.reverseOrder())));
        return resultado;
    }

    /** Cuántas notificaciones no fueron marcadas como leídas. */
    public static long sinLeer(List<Notificacion> notificaciones) {
        if (notificaciones == null) return 0;
        return notificaciones.stream().filter(n -> !n.leida()).count();
    }

    private static boolean esAsignada(String estado) {
        if (estado == null) return false;
        return switch (estado.toUpperCase()) {
            case "ASIGNADO", "EN_TRASLADO", "ENTREGADO" -> true;
            default -> false;
        };
    }

    private static String format(LocalDate date) {
        return date != null ? date.format(FECHA) : null;
    }

    private static String format(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(FECHA_HORA) : null;
    }
}
