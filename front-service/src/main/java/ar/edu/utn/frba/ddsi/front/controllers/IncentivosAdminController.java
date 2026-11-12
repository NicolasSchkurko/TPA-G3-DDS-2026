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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Pantallas de administración de incentivos.
 *
 * <p>Etapa 2: lecturas reales contra incentivos-service. Las escrituras
 * (POST/PUT/DELETE con el header Admin-Id) se agregan en la etapa 3.</p>
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
