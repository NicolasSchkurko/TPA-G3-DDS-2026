package ar.edu.utn.frba.ddsi.front.controllers;


import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import ar.edu.utn.frba.ddsi.front.dto.PersonaDonanteDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final LogisticasClient logisticasClient;
    private final DonacionesClient donacionesClient;

    public AdminController(LogisticasClient logisticasClient, DonacionesClient donacionesClient) {
        this.logisticasClient = logisticasClient;
        this.donacionesClient = donacionesClient;
    }

    @GetMapping("/admin")
    public String mostrarAdmin(
            @AuthenticationPrincipal OidcUser usuario,
            Model model) {

        agregarDatosDelAdmin(usuario, model);
        return "admin/admin-landing";
    }

    @GetMapping("/admin/donaciones")
    public String mostrarDonaciones() {
        return "redirect:/donaciones";
    }

    @GetMapping("/admin/entidades")
    public String mostrarEntidades() {
        return "redirect:/admin";
    }

    @GetMapping("/camiones")
    public String listarcamiones(Model model) {
        var camiones = logisticasClient.obtenerCamiones();
        model.addAttribute("camiones", camiones);
        return "admin/camiones";
    }

    // ---------------------------------------------------------------------
    // Administración de donantes
    // ---------------------------------------------------------------------

    /**
     * Listado completo de donantes. El filtro por nombre se resuelve en el navegador sobre esta
     * misma tabla (ver donantes.js): el endpoint /api/personas/buscar del backend devuelve un único
     * donante y no una lista, así que no sirve para un buscador incremental.
     */
    @GetMapping("/admin/donantes")
    public String mostrarDonantes(@AuthenticationPrincipal OidcUser usuario, Model model) {
        agregarDatosDelAdmin(usuario, model);
        List<PersonaDonanteDTO> donantes = donacionesClient.obtenerDonantes();
        model.addAttribute("donantes", donantes);
        return "admin/donantes";
    }

    @PostMapping("/admin/donantes/importar")
    public String importarDonantes(@RequestParam("archivo") MultipartFile archivo,
                                   RedirectAttributes redirect) {
        if (archivo == null || archivo.isEmpty()) {
            redirect.addFlashAttribute("error", "Elegí un archivo CSV antes de importar.");
            return "redirect:/admin/donantes";
        }

        String nombreArchivo = archivo.getOriginalFilename();
        if (nombreArchivo != null && !nombreArchivo.toLowerCase().endsWith(".csv")) {
            redirect.addFlashAttribute("error",
                    "El archivo tiene que ser un .csv (recibimos '" + nombreArchivo + "').");
            return "redirect:/admin/donantes";
        }

        try {
            UUID importId = donacionesClient.importarDonantesCSV(archivo.getBytes(), nombreArchivo);
            // La importación es asincrónica: el backend responde 202 y procesa en segundo plano,
            // así que el listado recién recargado puede todavía no incluir las filas nuevas.
            redirect.addFlashAttribute("exito",
                    "Importación encolada. Los donantes van a aparecer en el listado en unos segundos; "
                            + "actualizá la página para verlos.");
            if (importId != null) {
                redirect.addFlashAttribute("importId", importId.toString());
            }
        } catch (Exception e) {
            log.error("Falló la importación de donantes por CSV: {}", e.getMessage());
            redirect.addFlashAttribute("error", "No se pudo importar el archivo: " + e.getMessage());
        }
        return "redirect:/admin/donantes";
    }

    @PostMapping("/admin/donantes/{id}/actualizar")
    public String actualizarDonante(@PathVariable UUID id,
                                    @RequestParam(required = false) String nombre,
                                    @RequestParam(required = false) String apellido,
                                    @RequestParam(required = false) String razonSocial,
                                    @RequestParam(required = false) String telefono,
                                    @RequestParam(required = false) String correoElectronico,
                                    RedirectAttributes redirect) {
        Map<String, Object> cambios = new LinkedHashMap<>();
        agregarSiTieneTexto(cambios, "nombre", nombre);
        agregarSiTieneTexto(cambios, "apellido", apellido);
        agregarSiTieneTexto(cambios, "razonSocial", razonSocial);
        agregarSiTieneTexto(cambios, "telefono", telefono);
        agregarSiTieneTexto(cambios, "correoElectronico", correoElectronico);

        if (cambios.isEmpty()) {
            redirect.addFlashAttribute("error", "No se modificó ningún campo.");
            return "redirect:/admin/donantes";
        }

        try {
            donacionesClient.actualizarDonanteCampos(id, cambios);
            redirect.addFlashAttribute("exito", "Donante actualizado correctamente.");
        } catch (Exception e) {
            log.error("Falló la actualización del donante {}: {}", id, e.getMessage());
            redirect.addFlashAttribute("error", "No se pudo actualizar el donante: " + e.getMessage());
        }
        return "redirect:/admin/donantes";
    }

    @PostMapping("/admin/donantes/{id}/eliminar")
    public String eliminarDonante(@PathVariable UUID id, RedirectAttributes redirect) {
        try {
            donacionesClient.eliminarDonante(id);
            redirect.addFlashAttribute("exito", "Donante eliminado correctamente.");
        } catch (Exception e) {
            log.error("Falló la baja del donante {}: {}", id, e.getMessage());
            redirect.addFlashAttribute("error", "No se pudo eliminar el donante: " + e.getMessage());
        }
        return "redirect:/admin/donantes";
    }

    private static void agregarSiTieneTexto(Map<String, Object> destino, String clave, String valor) {
        if (valor != null && !valor.isBlank()) {
            destino.put(clave, valor.trim());
        }
    }

    /** Datos que consume la cabecera de admin (nombre, apellido y rol). */
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
