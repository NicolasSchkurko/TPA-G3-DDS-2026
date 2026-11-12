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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Pantallas de administración de incentivos.
 *
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
}
