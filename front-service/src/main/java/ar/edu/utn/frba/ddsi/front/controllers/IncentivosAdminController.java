package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.front.dto.incentivos.IncentivosAdminDTOs;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Pantallas de administración de incentivos.
 *
 * <p>Las operaciones administrativas se delegan a incentivos-service con el identificador
 * del administrador autenticado.</p>
 * <p>Las lecturas y la generación de rankings se realizan contra incentivos-service.</p>
 */
@Controller
public class IncentivosAdminController {

    private static final Logger log = LoggerFactory.getLogger(IncentivosAdminController.class);

    private final IncentivosClient incentivosClient;

    public IncentivosAdminController(IncentivosClient incentivosClient) {
        this.incentivosClient = incentivosClient;
    }

    @GetMapping("/admin/incentivos")
    public String misiones(@AuthenticationPrincipal OidcUser usuario, Model model) {
        agregarDatosDelAdmin(usuario, model);
        model.addAttribute("misiones", incentivosClient.obtenerMisiones());
        model.addAttribute("categorias", incentivosClient.obtenerCategorias());
        return "admin/incentivos-misiones";
    }

    @PostMapping("/admin/incentivos/misiones")
    public String crearMision(@ModelAttribute IncentivosAdminDTOs.Mision mision,
                              @AuthenticationPrincipal OidcUser usuario,
                              RedirectAttributes flash) {
        UUID idAdmin;
        try {
            idAdmin = UUID.fromString(usuario.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("El sub de Keycloak del administrador no es un UUID válido.");
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje",
                    "No se pudo identificar al administrador para guardar la misión.");
            return "redirect:/admin/incentivos";
        }

        try {
            incentivosClient.crearMision(idAdmin, mision);
            flash.addFlashAttribute("toastTipo", "success");
            flash.addFlashAttribute("toastMensaje", "La misión se creó correctamente.");
        } catch (RestClientException e) {
            log.error("No se pudo crear la misión desde incentivos-service: {}", e.getMessage());
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje",
                    "No se pudo crear la misión. Revisá los datos e intentá de nuevo.");
        }
        return "redirect:/admin/incentivos";
    }

    @PostMapping("/admin/incentivos/categorias")
    public String crearCategoria(@AuthenticationPrincipal OidcUser usuario,
                                 @RequestParam String nombre,
                                 @RequestParam(required = false) Integer posicionSecuencia,
                                 @RequestParam(required = false) List<UUID> misiones,
                                 RedirectAttributes redirectAttributes) {
        IncentivosAdminDTOs.Categoria categoria = nuevaCategoria(nombre, posicionSecuencia, misiones);
        try {
            incentivosClient.crearCategoria(categoria, idAdmin(usuario));
            notificar(redirectAttributes, "success", "La categoría se creó correctamente.");
        } catch (RestClientResponseException e) {
            notificarError(redirectAttributes, e);
        }
        return "redirect:/admin/incentivos";
    }

    @PostMapping("/admin/incentivos/categorias/{id}")
    public String actualizarCategoria(@PathVariable UUID id,
                                      @AuthenticationPrincipal OidcUser usuario,
                                      @RequestParam String nombre,
                                      @RequestParam(required = false) Integer posicionSecuencia,
                                      @RequestParam(required = false) List<UUID> misiones,
                                      RedirectAttributes redirectAttributes) {
        IncentivosAdminDTOs.Categoria categoria = nuevaCategoria(nombre, posicionSecuencia, misiones);
        try {
            incentivosClient.actualizarCategoria(id, categoria, idAdmin(usuario));
            notificar(redirectAttributes, "success", "La categoría se actualizó correctamente.");
        } catch (RestClientResponseException e) {
            notificarError(redirectAttributes, e);
        }
        return "redirect:/admin/incentivos";
    }

    @PostMapping("/admin/incentivos/categorias/{id}/eliminar")
    public String eliminarCategoria(@PathVariable UUID id,
                                    @AuthenticationPrincipal OidcUser usuario,
                                    RedirectAttributes redirectAttributes) {
        try {
            incentivosClient.eliminarCategoria(id, idAdmin(usuario));
            notificar(redirectAttributes, "success", "La categoría se eliminó correctamente.");
        } catch (RestClientResponseException e) {
            notificarError(redirectAttributes, e, "eliminar");
        }
        return "redirect:/admin/incentivos";
    }

    @PostMapping("/admin/incentivos/misiones/{id}")
    public String actualizarMision(@PathVariable UUID id,
                                   @ModelAttribute IncentivosAdminDTOs.Mision mision,
                                   @AuthenticationPrincipal OidcUser usuario,
                                   RedirectAttributes flash) {
        UUID idAdmin;
        try {
            idAdmin = UUID.fromString(usuario.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("El sub de Keycloak del administrador no es un UUID válido.");
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje",
                    "No se pudo identificar al administrador para guardar la misión.");
            return "redirect:/admin/incentivos";
        }

        try {
            incentivosClient.actualizarMision(idAdmin, id, mision);
            flash.addFlashAttribute("toastTipo", "success");
            flash.addFlashAttribute("toastMensaje", "La misión se actualizó correctamente.");
        } catch (RestClientException e) {
            log.error("No se pudo actualizar la misión {} desde incentivos-service: {}", id, e.getMessage());
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje",
                    "No se pudo actualizar la misión. Revisá los datos e intentá de nuevo.");
        }
        return "redirect:/admin/incentivos";
    }

    @PostMapping("/admin/incentivos/misiones/{id}/eliminar")
    public String eliminarMision(@PathVariable UUID id,
                                 @AuthenticationPrincipal OidcUser usuario,
                                 RedirectAttributes flash) {
        try {
            incentivosClient.eliminarMision(idAdmin(usuario), id);
            flash.addFlashAttribute("toastTipo", "success");
            flash.addFlashAttribute("toastMensaje", "La misión se dio de baja correctamente.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("No se pudo identificar al administrador para eliminar la misión {}: {}",
                    id, e.getMessage());
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje",
                    "No se pudo identificar al administrador para eliminar la misión.");
        } catch (RestClientResponseException e) {
            log.warn("No se pudo eliminar la misión {} desde incentivos-service ({}): {}",
                    id, e.getStatusCode(), e.getResponseBodyAsString());
            flash.addFlashAttribute("toastTipo", "error");
            flash.addFlashAttribute("toastMensaje",
                    "No se pudo dar de baja la misión. " + e.getResponseBodyAsString());
        }
        return "redirect:/admin/incentivos";
    }

    @GetMapping("/admin/incentivos/rankings")
    public String rankings(@RequestParam(value = "periodo", required = false) String periodo,
                           @AuthenticationPrincipal OidcUser usuario,
                           Model model) {
        agregarDatosDelAdmin(usuario, model);

        List<IncentivosAdminDTOs.RankingMes> rankings = incentivosClient.obtenerRankings();

        IncentivosAdminDTOs.RankingMes seleccionado = rankings.stream()
                .filter(r -> Objects.equals(r.getPeriodo(), periodo))
                .findFirst()
                .orElse(null);

        boolean vigente = seleccionado != null
                && YearMonth.now().toString().equals(seleccionado.getPeriodo());

        String periodosExistentes = rankings.stream()
                .map(IncentivosAdminDTOs.RankingMes::getPeriodo)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));

        model.addAttribute("rankings", rankings);
        model.addAttribute("seleccionado", seleccionado);
        model.addAttribute("vigente", vigente);
        model.addAttribute("periodosExistentes", periodosExistentes);

        return "admin/incentivos-rankings";
    }

    @PostMapping("/admin/incentivos/rankings")
    public String crearRanking(@RequestParam YearMonth periodo,
                               @AuthenticationPrincipal OidcUser usuario,
                               RedirectAttributes redirect) {
        try {
            UUID idAdmin = UUID.fromString(usuario.getSubject());
            incentivosClient.crearRanking(periodo, idAdmin);
            redirect.addFlashAttribute("toastTipo", "success");
            redirect.addFlashAttribute("toastMensaje",
                    "Ranking mensual " + periodo + " generado correctamente.");
        } catch (RestClientResponseException e) {
            log.error("incentivos-service rechazó el ranking del período {} con estado {}",
                    periodo, e.getStatusCode(), e);
            informarErrorCreacionRanking(redirect);
        } catch (RestClientException | IllegalArgumentException e) {
            log.error("No se pudo generar el ranking del período {}", periodo, e);
            informarErrorCreacionRanking(redirect);
        }
        redirect.addAttribute("periodo", periodo);
        return "redirect:/admin/incentivos/rankings";
    }

    @PostMapping("/admin/incentivos/rankings/{idRanking}/eliminar")
    public String eliminarRanking(@PathVariable UUID idRanking,
                                  @RequestParam(required = false) YearMonth periodo,
                                  @AuthenticationPrincipal OidcUser usuario,
                                  RedirectAttributes redirect) {
        try {
            UUID idAdmin = UUID.fromString(usuario.getSubject());
            incentivosClient.eliminarRanking(idRanking, idAdmin);
            redirect.addFlashAttribute("toastTipo", "success");
            redirect.addFlashAttribute("toastMensaje", "El ranking se eliminó correctamente.");
        } catch (RestClientResponseException e) {
            log.error("incentivos-service rechazó la eliminación del ranking {} con estado {}",
                    idRanking, e.getStatusCode(), e);
            informarErrorEliminacionRanking(redirect);
        } catch (RestClientException | IllegalArgumentException e) {
            log.error("No se pudo eliminar el ranking {}", idRanking, e);
            informarErrorEliminacionRanking(redirect);
        }
        if (periodo != null) {
            redirect.addAttribute("periodo", periodo);
        }
        return "redirect:/admin/incentivos/rankings";
    }

    private static void informarErrorCreacionRanking(RedirectAttributes redirect) {
        redirect.addFlashAttribute("toastTipo", "error");
        redirect.addFlashAttribute("toastMensaje",
                "No se pudo generar el ranking. Verificá que el período esté cerrado y que no exista ya.");
    }

    private static void informarErrorEliminacionRanking(RedirectAttributes redirect) {
        redirect.addFlashAttribute("toastTipo", "error");
        redirect.addFlashAttribute("toastMensaje",
                "No se pudo eliminar el ranking. Actualizá la página e intentá nuevamente.");
    }

    private void agregarDatosDelAdmin(OidcUser usuario, Model model) {
        if (usuario == null) {
            model.addAttribute("nombre", "");
            model.addAttribute("apellido", "");
            model.addAttribute("rol", "Usuario");
            return;
        }

        model.addAttribute("nombre", usuario.getGivenName());
        model.addAttribute("apellido", usuario.getFamilyName());
        model.addAttribute("username", usuario.getPreferredUsername());

        boolean esAdministrador = usuario.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        model.addAttribute("rol", esAdministrador ? "Administrador" : "Usuario");
    }

    private static IncentivosAdminDTOs.Categoria nuevaCategoria(String nombre,
                                                                Integer posicionSecuencia,
                                                                List<UUID> misiones) {
        IncentivosAdminDTOs.Categoria categoria = new IncentivosAdminDTOs.Categoria();
        categoria.setNombre(nombre.trim());
        categoria.setPosicionSecuencia(posicionSecuencia);
        categoria.setMisiones(misiones == null ? List.of() : misiones);
        return categoria;
    }

    private static UUID idAdmin(OidcUser usuario) {
        if (usuario == null || usuario.getSubject() == null || usuario.getSubject().isBlank()) {
            throw new IllegalStateException("No se pudo identificar al administrador autenticado.");
        }
        return UUID.fromString(usuario.getSubject());
    }

    private static void notificarError(RedirectAttributes attributes, RestClientResponseException error) {
        notificarError(attributes, error, "guardar");
    }

    private static void notificarError(RedirectAttributes attributes,
                                       RestClientResponseException error,
                                       String accion) {
        String detalle = error.getResponseBodyAsString();
        notificar(attributes, "error", detalle == null || detalle.isBlank()
                ? "No se pudo " + accion + " la categoría (" + error.getStatusCode() + ")."
                : "No se pudo " + accion + " la categoría: " + detalle);
    }

    private static void notificar(RedirectAttributes attributes, String tipo, String mensaje) {
        attributes.addFlashAttribute("toastTipo", tipo);
        attributes.addFlashAttribute("toastMensaje", mensaje);
    }
}
