package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.front.dto.NecesidadDTO;
import ar.edu.utn.frba.ddsi.front.dto.SeguimientoDTO;
import ar.edu.utn.frba.ddsi.front.dto.logistica.LogisticaDTOs;
import ar.edu.utn.frba.ddsi.front.services.DemoDataService;
import ar.edu.utn.frba.ddsi.front.services.EntidadContext;
import ar.edu.utn.frba.ddsi.front.services.IdentidadService;
import ar.edu.utn.frba.ddsi.front.services.NotificacionesDerivadasService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/entidad")
public class EntidadViewController {

    private static final Logger log = LoggerFactory.getLogger(EntidadViewController.class);

    private final EntidadContext entidadContext;
    private final IdentidadService identidadService;
    private final DonacionesClient donacionesClient;
    private final LogisticasClient logisticasClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public EntidadViewController(EntidadContext entidadContext,
                                 IdentidadService identidadService,
                                 DonacionesClient donacionesClient,
                                 LogisticasClient logisticasClient) {
        this.entidadContext = entidadContext;
        this.identidadService = identidadService;
        this.donacionesClient = donacionesClient;
        this.logisticasClient = logisticasClient;
    }

    /* ============================ Dashboard ============================ */

    @GetMapping
    public String dashboard(Authentication authentication, HttpServletRequest request, Model model) {
        DatosPortal datos = prepararPortal(authentication, request, model);
        if (datos == null) return "entidad/vincular-perfil";

        SeguimientoDTO seguimiento = datos.seguimiento();
        model.addAttribute("seguimientoJson", toJson(mapDataSeguimiento(datos.entidad(), seguimiento, datos.demo())));
        model.addAttribute("entregasActivas", seguimiento.getEntregas());
        model.addAttribute("camiones", seguimiento.getCamiones());
        model.addAttribute("necesidadesDestacadas",
                datos.necesidades().stream().filter(n -> n.getActiva() == null || n.getActiva()).limit(3).toList());
        model.addAttribute("necesidadesTop",
                datos.necesidades().stream().filter(n -> n.getActiva() == null || n.getActiva()).limit(2).toList());
        model.addAttribute("necesidadesActivas",
                datos.necesidades().stream().filter(n -> n.getActiva() == null || n.getActiva()).count());
        model.addAttribute("totalDonaciones", datos.donaciones().size());
        model.addAttribute("proximaEntrega", proximaEntrega(seguimiento.getEntregas()));
        model.addAttribute("enCamino",
                seguimiento.getEntregas().stream().filter(e -> "EN_CAMINO".equalsIgnoreCase(e.getEstado())).count());
        model.addAttribute("confirmadas",
                datos.donaciones().stream().filter(d -> "ENTREGADO".equalsIgnoreCase(d.getEstado())).count());
        return "entidad/dashboard";
    }

    /* ============================ Necesidades ============================ */

    @GetMapping("/necesidades")
    public String necesidades(Authentication authentication, HttpServletRequest request, Model model) {
        DatosPortal datos = prepararPortal(authentication, request, model);
        if (datos == null) return "entidad/vincular-perfil";
        model.addAttribute("necesidades", datos.necesidades());
        model.addAttribute("catalogoJson", toJson(catalogoNecesidades()));
        return "entidad/necesidades";
    }

    @PostMapping("/necesidades")
    public String crearNecesidad(@RequestParam Map<String, String> form,
                                 Authentication authentication, HttpServletRequest request,
                                 RedirectAttributes flash) {
        UUID entidadId = entidadContext.entidadId(request, authentication);
        if (entidadId == null) {
            flash.addFlashAttribute("toastTipo", "info");
            flash.addFlashAttribute("toastMensaje", "Vinculá tu perfil de entidad para registrar necesidades.");
            return "redirect:/entidad/vincular";
        }
        if (DemoDataService.ID_DEMO.equals(entidadId)) {
            flash.addFlashAttribute("toastTipo", "info");
            flash.addFlashAttribute("toastMensaje", "Modo demostración: la necesidad no se guardó.");
            return "redirect:/entidad/necesidades";
        }
        try {
            NecesidadDTO dto = necesidadDesdeForm(form);
            donacionesClient.crearNecesidad(entidadId, dto);
            flash.addFlashAttribute("toastTipo", "success");
            flash.addFlashAttribute("toastMensaje", "La necesidad se registró correctamente.");
        } catch (Exception e) {
            log.warn("No se pudo crear la necesidad: {}", e.getMessage());
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", "No se pudo registrar la necesidad. Revisá los datos e intentá de nuevo.");
        }
        return "redirect:/entidad/necesidades";
    }

    @PostMapping("/necesidades/{id}")
    public String actualizarNecesidad(@PathVariable UUID id, @RequestParam Map<String, String> form,
                                      Authentication authentication, HttpServletRequest request,
                                      RedirectAttributes flash) {
        UUID entidadId = entidadContext.entidadId(request, authentication);
        if (entidadId == null || DemoDataService.ID_DEMO.equals(entidadId)) {
            flash.addFlashAttribute("toastTipo", "info");
            flash.addFlashAttribute("toastMensaje", "Modo demostración: los cambios no se guardaron.");
            return "redirect:/entidad/necesidades";
        }
        try {
            NecesidadDTO dto = necesidadDesdeForm(form);
            donacionesClient.actualizarNecesidad(entidadId, id, dto);
            flash.addFlashAttribute("toastTipo", "success");
            flash.addFlashAttribute("toastMensaje", "La necesidad se actualizó correctamente.");
        } catch (Exception e) {
            log.warn("No se pudo actualizar la necesidad {}: {}", id, e.getMessage());
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", "No se pudo actualizar la necesidad. Intentá de nuevo.");
        }
        return "redirect:/entidad/necesidades";
    }

    @PostMapping("/necesidades/{id}/eliminar")
    public String eliminarNecesidad(@PathVariable UUID id,
                                    Authentication authentication, HttpServletRequest request,
                                    RedirectAttributes flash) {
        UUID entidadId = entidadContext.entidadId(request, authentication);
        if (entidadId == null || DemoDataService.ID_DEMO.equals(entidadId)) {
            flash.addFlashAttribute("toastTipo", "info");
            flash.addFlashAttribute("toastMensaje", "Modo demostración: la necesidad no se eliminó.");
            return "redirect:/entidad/necesidades";
        }
        try {
            donacionesClient.eliminarNecesidad(entidadId, id);
            flash.addFlashAttribute("toastTipo", "success");
            flash.addFlashAttribute("toastMensaje", "La necesidad se dio de baja.");
        } catch (Exception e) {
            log.warn("No se pudo eliminar la necesidad {}: {}", id, e.getMessage());
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", "No se pudo eliminar la necesidad. Intentá de nuevo.");
        }
        return "redirect:/entidad/necesidades";
    }

    /* ============================ Donaciones ============================ */

    @GetMapping("/donaciones")
    public String donaciones(Authentication authentication, HttpServletRequest request, Model model) {
        DatosPortal datos = prepararPortal(authentication, request, model);
        if (datos == null) return "entidad/vincular-perfil";
        model.addAttribute("donaciones", datos.donaciones());
        model.addAttribute("entregas", datos.entregas());
        return "entidad/donaciones";
    }

    @GetMapping("/donaciones/{id}")
    public String detalleDonacion(@PathVariable UUID id,
                                  Authentication authentication, HttpServletRequest request, Model model,
                                  RedirectAttributes flash) {
        DatosPortal datos = prepararPortal(authentication, request, model);
        if (datos == null) return "entidad/vincular-perfil";

        DonacionDTO donacion = datos.donaciones().stream()
                .filter(d -> id.equals(d.getId()))
                .findFirst()
                .orElse(null);
        if (donacion == null && datos.demo()) {
            donacion = datos.donaciones().stream().findFirst().orElse(null);
        }
        if (donacion == null) {
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", "No se encontró la donación en tu entidad.");
            return "redirect:/entidad/donaciones";
        }

        LogisticaDTOs.Bien entrega = datos.entregas().get(donacion.getId());
        model.addAttribute("donacion", donacion);
        model.addAttribute("entrega", entrega);
        model.addAttribute("timeline", timeline(donacion, entrega));
        model.addAttribute("puedeConfirmar", entrega != null
                && entrega.getEstado() != null
                && ("EN_CAMINO".equalsIgnoreCase(entrega.getEstado()) || "PENDIENTE".equalsIgnoreCase(entrega.getEstado())));
        model.addAttribute("yaConfirmada", entrega != null && "ENTREGADA".equalsIgnoreCase(entrega.getEstado()));
        return "entidad/donacion-detalle";
    }

    @PostMapping("/donaciones/{id}/confirmar")
    public String confirmarRecepcion(@PathVariable UUID id,
                                     @RequestParam("fotoBase64") String fotoBase64,
                                     @RequestParam(value = "observacion", required = false) String observacion,
                                     Authentication authentication, HttpServletRequest request,
                                     RedirectAttributes flash) {
        DatosPortal datos = prepararPortal(authentication, request, null);
        if (datos == null) return "redirect:/entidad/vincular";

        DonacionDTO donacion = datos.donaciones().stream()
                .filter(d -> id.equals(d.getId())).findFirst().orElse(null);
        if (donacion == null) {
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", "No se encontró la donación en tu entidad.");
            return "redirect:/entidad/donaciones";
        }

        if (fotoBase64 == null || fotoBase64.length() < 100) {
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", "Adjuntá una foto de la donación recibida.");
            return "redirect:/entidad/donaciones/" + id;
        }
        if (fotoBase64.length() > 5_500_000) {
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", "La foto es demasiado grande. Probá con una imagen más liviana.");
            return "redirect:/entidad/donaciones/" + id;
        }

        if (datos.demo()) {
            flash.addFlashAttribute("toastTipo", "info");
            flash.addFlashAttribute("toastMensaje", "Modo demostración: la confirmación no se envió al backend.");
            return "redirect:/entidad/donaciones/" + id;
        }

        LogisticaDTOs.ActualizacionEntrega actualizacion = new LogisticaDTOs.ActualizacionEntrega();
        actualizacion.setEstado("ENTREGADA");
        actualizacion.setFotoUrl(fotoBase64);
        actualizacion.setJustificacion(observacion != null && !observacion.isBlank() ? observacion : null);

        try {
            logisticasClient.actualizarEstadoEntrega(id, actualizacion);
            flash.addFlashAttribute("toastTipo", "success");
            flash.addFlashAttribute("toastMensaje", "¡Recepción confirmada! Gracias por avisarnos.");
        } catch (Exception e) {
            log.warn("No se pudo confirmar la recepción de {}: {}", id, e.getMessage());
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", "No se pudo confirmar la recepción. Intentá de nuevo.");
        }
        return "redirect:/entidad/donaciones/" + id;
    }

    /* ============================ Seguimiento ============================ */

    @GetMapping("/seguimiento")
    public String seguimiento(Authentication authentication, HttpServletRequest request, Model model) {
        DatosPortal datos = prepararPortal(authentication, request, model);
        if (datos == null) return "entidad/vincular-perfil";
        model.addAttribute("seguimientoJson", toJson(mapDataSeguimiento(datos.entidad(), datos.seguimiento(), datos.demo())));
        model.addAttribute("camiones", datos.seguimiento().getCamiones());
        model.addAttribute("entregasActivas", datos.seguimiento().getEntregas());
        model.addAttribute("pollingMs", 30000);
        return "entidad/seguimiento";
    }

    /* ============================ Notificaciones ============================ */

    @GetMapping("/notificaciones")
    public String notificaciones(@RequestParam(value = "filtro", defaultValue = "todas") String filtro,
                                 Authentication authentication, HttpServletRequest request,
                                 HttpSession session, Model model) {
        DatosPortal datos = prepararPortal(authentication, request, model);
        if (datos == null) return "entidad/vincular-perfil";
        List<NotificacionesDerivadasService.Notificacion> sistema =
                entidadContext.notificacionesServicio(request, authentication, session);
        List<NotificacionesDerivadasService.Notificacion> actividad =
                entidadContext.actividad(request, authentication, session);
        List<NotificacionesDerivadasService.Notificacion> todas = new ArrayList<>();
        todas.addAll(sistema);
        todas.addAll(actividad);

        boolean soloNoLeidas = "noLeidas".equalsIgnoreCase(filtro);
        if (soloNoLeidas) {
            sistema = sistema.stream().filter(n -> !n.leida()).toList();
            actividad = actividad.stream().filter(n -> !n.leida()).toList();
        }

        model.addAttribute("notificacionesSistema", sistema);
        model.addAttribute("actividad", actividad);
        model.addAttribute("notificaciones", todas);
        model.addAttribute("filtro", soloNoLeidas ? "noLeidas" : "todas");
        model.addAttribute("sinLeer", NotificacionesDerivadasService.sinLeer(todas));
        return "entidad/notificaciones";
    }

    @PostMapping("/notificaciones/{id}/leida")
    public String marcarLeida(@PathVariable String id,
                              Authentication authentication, HttpServletRequest request,
                              HttpSession session, RedirectAttributes flash) {
        Set<String> leidas = entidadContext.leidasEnSesion(session);
        leidas.add(id);
        return "redirect:/entidad/notificaciones";
    }

    @PostMapping("/notificaciones/todas-leidas")
    public String marcarTodasLeidas(Authentication authentication, HttpServletRequest request,
                                    HttpSession session, RedirectAttributes flash) {
        List<NotificacionesDerivadasService.Notificacion> notificaciones =
                entidadContext.notificaciones(request, authentication, session);
        Set<String> leidas = entidadContext.leidasEnSesion(session);
        notificaciones.forEach(n -> leidas.add(n.id()));
        flash.addFlashAttribute("toastTipo", "success");
        flash.addFlashAttribute("toastMensaje", "Todas las notificaciones quedaron marcadas como leídas.");
        return "redirect:/entidad/notificaciones";
    }

    /* ============================ Vincular perfil ============================ */

    @GetMapping("/vincular")
    public String vincular(Authentication authentication, HttpServletRequest request, Model model) {
        if (entidadContext.entidad(request, authentication) != null) {
            return "redirect:/entidad";
        }
        model.addAttribute("entidades", identidadService.listarEntidadesParaVincular());
        return "entidad/vincular-perfil";
    }

    @PostMapping("/vincular")
    public String vincular(@RequestParam("entidadId") UUID entidadId,
                           Authentication authentication,
                           RedirectAttributes flash) {
        try {
            String sub = subDe(authentication);
            if (sub == null) {
                throw new IllegalArgumentException("Tu cuenta no tiene un identificador de Keycloak. Iniciá sesión nuevamente.");
            }
            identidadService.vincularEntidad(sub, entidadId);
            flash.addFlashAttribute("toastTipo", "success");
            flash.addFlashAttribute("toastMensaje", "Tu cuenta quedó vinculada a la entidad.");
            return "redirect:/entidad";
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", e.getMessage());
            return "redirect:/entidad/vincular";
        } catch (org.springframework.web.client.RestClientResponseException e) {
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", e.getStatusCode().value() == 409
                    ? "Esa entidad ya está vinculada a otra cuenta."
                    : "No se pudo vincular. Revisá los datos e intentá de nuevo.");
            return "redirect:/entidad/vincular";
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(EntidadViewController.class)
                    .warn("No se pudo vincular la entidad: {}", e.getMessage());
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje", "No se pudo vincular. Intentá nuevamente más tarde.");
            return "redirect:/entidad/vincular";
        }
    }

    /* ============================ Helpers ============================ */

    /** Identificador inmutable de la cuenta de Keycloak del usuario autenticado. */
    private static String subDe(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
            String sub = oidcUser.getSubject();
            return (sub != null && !sub.isBlank()) ? sub.trim() : null;
        }
        return null;
    }

    /**
     * Resuelve los datos del portal (reales o demo) y los expone en el modelo.
     * Devuelve null si el usuario todavía no vinculó su entidad (se muestra el onboarding).
     */
    private DatosPortal prepararPortal(Authentication authentication, HttpServletRequest request, Model model) {
        EntidadBeneficiariaDTO entidad = entidadContext.entidad(request, authentication);
        boolean tieneVinculo = entidadContext.entidadId(request, authentication) != null;
        boolean demo = entidad == null;

        if (demo && !tieneVinculo) {
            entidad = DemoDataService.entidadDemo();
        }

        if (entidad == null) {
            // Hay vínculo pero el backend no respondió: mostramos demo con aviso.
            entidad = DemoDataService.entidadDemo();
            demo = true;
        }

        List<DonacionDTO> donaciones = demo && DemoDataService.ID_DEMO.equals(entidad.getId())
                ? DemoDataService.donacionesDemo()
                : entidadContext.donaciones(request, authentication);
        Map<UUID, LogisticaDTOs.Bien> entregas = entidadContext.entregasPorDonacion(request);
        SeguimientoDTO seguimiento = entidadContext.seguimiento(request, authentication);
        if (demo && DemoDataService.ID_DEMO.equals(entidad.getId())) {
            seguimiento = DemoDataService.seguimientoDemo();
            entregas = new LinkedHashMap<>();
        }
        List<NecesidadDTO> necesidades = entidad.getNecesidades() != null ? entidad.getNecesidades() : List.of();

        if (model != null) {
            model.addAttribute("entidad", entidad);
            model.addAttribute("vinculado", tieneVinculo);
            model.addAttribute("demo", demo);
            model.addAttribute("ubicacionSimulada", seguimiento.isUbicacionSimulada());
            model.addAttribute("donaciones", donaciones);
            model.addAttribute("necesidades", necesidades);
        }
        return new DatosPortal(entidad, donaciones, entregas, seguimiento, demo, necesidades);
    }

    private Map<String, Object> mapDataSeguimiento(EntidadBeneficiariaDTO entidad, SeguimientoDTO seguimiento, boolean demo) {
        double[] coords = ar.edu.utn.frba.ddsi.front.services.SeguimientoService
                .centroDe(entidad.getDireccion() != null ? entidad.getDireccion().getCiudad() : null);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ubicacionSimulada", seguimiento.isUbicacionSimulada());
        data.put("generadoEn", seguimiento.getGeneradoEn());
        data.put("demo", demo);
        boolean tieneCoordenadas = entidad.getDireccion() != null
                && entidad.getDireccion().getLatitud() != null
                && entidad.getDireccion().getLongitud() != null;
        data.put("entidad", Map.of(
                "razonSocial", entidad.getRazonSocial() != null ? entidad.getRazonSocial() : "Mi entidad",
                "direccion", entidad.getDireccion() != null ? entidad.getDireccion().etiqueta() : "Sin dirección",
                "zona", entidad.getDireccion() != null && entidad.getDireccion().getCiudad() != null ? entidad.getDireccion().getCiudad() : "CABA",
                "lat", tieneCoordenadas ? entidad.getDireccion().getLatitud() : coords[0],
                "lng", tieneCoordenadas ? entidad.getDireccion().getLongitud() : coords[1]));
        data.put("entregas", seguimiento.getEntregas());
        data.put("camiones", seguimiento.getCamiones());
        data.put("depositoLat", seguimiento.getDepositoLat());
        data.put("depositoLng", seguimiento.getDepositoLng());
        data.put("depositoEtiqueta", seguimiento.getDepositoEtiqueta());
        return data;
    }

    /** Primera entrega activa (EN_CAMINO > PENDIENTE > resto). */
    private static SeguimientoDTO.EntregaActiva proximaEntrega(List<SeguimientoDTO.EntregaActiva> entregas) {
        if (entregas == null || entregas.isEmpty()) return null;
        return entregas.stream().filter(e -> "EN_CAMINO".equalsIgnoreCase(e.getEstado())).findFirst()
                .or(() -> entregas.stream().filter(e -> "PENDIENTE".equalsIgnoreCase(e.getEstado())).findFirst())
                .orElse(entregas.get(0));
    }

    /** Catálogo curado de categorías/subcategorías; se reemplaza por el endpoint real cuando exista. */
    private Map<String, List<String>> catalogoNecesidades() {
        Map<String, List<String>> catalogo = new LinkedHashMap<>();
        catalogo.put("Alimentos", List.of("Arroz", "Fideos", "Leche en polvo", "Aceite", "Legumbres", "Enlatados", "Harina"));
        catalogo.put("Textil", List.of("Ropa de abrigo", "Frazadas", "Calzado", "Ropa para bebés"));
        catalogo.put("Salud", List.of("Medicamentos", "Botiquín", "Insumos descartables"));
        catalogo.put("Higiene", List.of("Jabón", "Pañales", "Toallas", "Lavandina"));
        catalogo.put("Educación", List.of("Útiles escolares", "Libros", "Mochilas"));
        catalogo.put("Hogar", List.of("Muebles", "Electrodomésticos", "Ropa de cama"));
        return catalogo;
    }

    private static NecesidadDTO necesidadDesdeForm(Map<String, String> form) {
        NecesidadDTO dto = new NecesidadDTO();
        dto.setTipoNecesidad(blankToNull(form.get("tipoNecesidad")) != null ? form.get("tipoNecesidad") : "RECURRENTE");
        dto.setDescripcion(blankToNull(form.get("descripcion")));
        dto.setCantidadObjetivo(parseInteger(form.get("cantidadObjetivo"), "La cantidad objetivo es obligatoria y debe ser mayor a 0."));
        dto.setNombreCategoria(blankToNull(form.get("nombreCategoria")));
        dto.setNombreSubcategoria(blankToNull(form.get("nombreSubcategoria")));
        dto.setPlazoEnDias(parsePlazo(form.get("plazoEnDias")));
        dto.setActiva(true);
        if (dto.getNombreCategoria() == null || dto.getNombreSubcategoria() == null) {
            throw new IllegalArgumentException("Elegí una categoría y una subcategoría.");
        }
        return dto;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Integer parseInteger(String value, String message) {
        try {
            int parsed = Integer.parseInt(value != null ? value.trim() : "");
            if (parsed <= 0) throw new IllegalArgumentException(message);
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(message);
        }
    }

    private static Integer parsePlazo(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<TimelineItem> timeline(DonacionDTO donacion, LogisticaDTOs.Bien entrega) {
        List<TimelineItem> items = new ArrayList<>();
        items.add(new TimelineItem(
                donacion.getFechaEntrega() != null ? donacion.getFechaEntrega().toString() : null,
                "Estado actual de la donación",
                "La donación figura como " + (donacion.getEstado() != null ? donacion.getEstado() : "sin estado") + "."));
        if (entrega != null) {
            items.add(new TimelineItem(
                    entrega.getFechaCambioEstado() != null ? entrega.getFechaCambioEstado().toString() : null,
                    "Estado de logística: " + entrega.getEstado(),
                    "Cantidad: " + entrega.getCantidad() + " " + (entrega.getUnidadDeMedida() != null ? entrega.getUnidadDeMedida() : "")));
            if (entrega.getEventos() != null) {
                for (LogisticaDTOs.EventoLogistica evento : entrega.getEventos()) {
                    items.add(new TimelineItem(null,
                            "Evento: " + (evento.getTipoEvento() != null ? evento.getTipoEvento() : "actualización"),
                            evento.getJustificacion() != null ? evento.getJustificacion() : evento.getReferenciaId()));
                }
            }
            if (entrega.getFotoComprobante() != null && !entrega.getFotoComprobante().isBlank()) {
                items.add(new TimelineItem(null, "Foto de recepción cargada", "Comprobante disponible en el depósito."));
            }
        }
        return items;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("No se pudo serializar a JSON: {}", e.getMessage());
            return "{}";
        }
    }

    private record DatosPortal(EntidadBeneficiariaDTO entidad,
                               List<DonacionDTO> donaciones,
                               Map<UUID, LogisticaDTOs.Bien> entregas,
                               SeguimientoDTO seguimiento,
                               boolean demo,
                               List<NecesidadDTO> necesidades) {
    }

    public record TimelineItem(String fecha, String titulo, String detalle) {
    }
}
