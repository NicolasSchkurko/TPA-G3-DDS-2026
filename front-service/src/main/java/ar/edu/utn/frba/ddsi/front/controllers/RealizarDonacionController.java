package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.dto.FormularioDonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.PersonaDonanteDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Optional;

@Controller
public class RealizarDonacionController {

    private static final Logger log = LoggerFactory.getLogger(RealizarDonacionController.class);

    private final DonacionesClient donacionesClient;

    public RealizarDonacionController(DonacionesClient donacionesClient) {
        this.donacionesClient = donacionesClient;
    }

    @GetMapping("/realizar-donacion")
    public String mostrarFormulario(Authentication authentication,
                                    Model model) {
        if (!tieneRolDonante(authentication)) {
            model.addAttribute("error", "Tu cuenta no tiene el rol de donante.");
            model.addAttribute("mostrarRegistro", true);
            return "donacion-perfil-error";
        }
        try {
            Optional<PersonaDonanteDTO> donante = obtenerPerfilDonante(authentication, model);
            if (donante.isEmpty()) {
                return "donacion-perfil-error";
            }

            model.addAttribute("donante", donante.get());
            FormularioDonacionDTO formulario = new FormularioDonacionDTO();
            formulario.setIdDonante(donante.get().getId());
            // La fecha de realización no es editable: se fija al día de hoy.
            formulario.setFechaRealizacion(java.time.LocalDate.now());
            model.addAttribute("formulario", formulario);
            prepararCatalogo(model);
            return "realizar-donacion";
        } catch (RestClientResponseException | ResourceAccessException e) {
            log.error("No se pudo consultar el perfil del donante en el servicio de donaciones", e);
            model.addAttribute("error", "No se pudo verificar tu perfil. Intentá nuevamente más tarde.");
            model.addAttribute("mostrarRegistro", false);
            return "donacion-perfil-error";
        }
    }

    @GetMapping("/donacion-exitosa")
    public String mostrarDonacionExitosa() {
        return "donacion-exitosa";
    }

    @PostMapping("/realizar-donacion")
    public String crearDonacion(@Valid @ModelAttribute("formulario") FormularioDonacionDTO formulario,
                                BindingResult bindingResult,
                                Authentication authentication,
                                Model model) {
        Optional<PersonaDonanteDTO> donante;
        try {
            donante = obtenerPerfilDonante(authentication, model);
        } catch (RestClientResponseException | ResourceAccessException e) {
            log.error("No se pudo consultar el perfil del donante en el servicio de donaciones", e);
            model.addAttribute("error", "No se pudo verificar tu perfil. Intentá nuevamente más tarde.");
            model.addAttribute("mostrarRegistro", false);
            return "donacion-perfil-error";
        }

        if (donante.isEmpty()) {
            return "donacion-perfil-error";
        }

        formulario.setIdDonante(donante.get().getId());
        // La fecha de realización no es editable: se fuerza al día de hoy.
        formulario.setFechaRealizacion(java.time.LocalDate.now());
        model.addAttribute("donante", donante.get());
        prepararCatalogo(model);
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "realizar-donacion";
        }

        try {
            donacionesClient.crearDonacion(formulario);
            return "redirect:/donacion-exitosa";
        } catch (RestClientResponseException e) {
            log.warn("El servicio de donaciones rechazó el formulario con estado {}", e.getStatusCode());
            model.addAttribute("error", e.getStatusCode().is4xxClientError()
                    ? "No se pudo registrar la donación. Verificá los datos ingresados."
                    : "El servicio de donaciones no está disponible. Intentá nuevamente más tarde.");
        } catch (ResourceAccessException e) {
            log.error("No se pudo conectar con el servicio de donaciones", e);
            model.addAttribute("error", "No se pudo conectar con el servicio de donaciones. Intentá nuevamente más tarde.");
        }
        return "realizar-donacion";
    }

    private Optional<PersonaDonanteDTO> obtenerPerfilDonante(Authentication authentication, Model model) {
        boolean esDonante = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_DONANTE".equals(authority.getAuthority()));
        if (!esDonante) {
            model.addAttribute("error", "Tu cuenta no tiene el rol de donante.");
            model.addAttribute("mostrarRegistro", true);
            return Optional.empty();
        }

        String correo = obtenerCorreo(authentication);
        if (correo == null || correo.isBlank()) {
            model.addAttribute("error", "No encontramos un perfil de donante asociado al correo de tu cuenta.");
            model.addAttribute("mostrarRegistro", true);
            return Optional.empty();
        }

        // La identidad estable es el sub de Keycloak: funciona aunque el email del perfil
        // sea distinto (por ejemplo, una cuenta chofer que además es donante).
        String sub = obtenerSub(authentication);
        var identidad = sub != null ? donacionesClient.buscarIdentidadPorSub(sub) : null;
        if (identidad == null) {
            identidad = donacionesClient.buscarIdentidadPorEmail(correo);
        }
        if (identidad != null && identidad.getDonanteId() != null) {
            PersonaDonanteDTO perfil = donacionesClient.obtenerDonantePorId(identidad.getDonanteId());
            if (perfil != null) {
                return Optional.of(perfil);
            }
        }

        List<PersonaDonanteDTO> donantes = donacionesClient.buscarDonantesPorCorreo(correo);
        if (donantes.isEmpty()) {
            model.addAttribute("error", "El correo de tu cuenta no coincide con el correo de contacto de ningún perfil de donante.");
            model.addAttribute("mostrarRegistro", true);
            return Optional.empty();
        }
        if (donantes.size() > 1) {
            model.addAttribute("error", "Hay más de un perfil de donante asociado al correo de tu cuenta. Contactá al equipo de soporte para que puedan corregir la asociación.");
            model.addAttribute("mostrarRegistro", false);
            return Optional.empty();
        }
        return Optional.of(donantes.get(0));
    }

    private void prepararCatalogo(Model model) {
        java.util.Map<String, java.util.List<String>> catalogo = donacionesClient.obtenerCatalogoCategorias();
        if (catalogo == null || catalogo.isEmpty()) {
            catalogo = new java.util.LinkedHashMap<>();
            catalogo.put("Alimentos", java.util.List.of("Arroz", "Fideos", "Leche en polvo", "Aceite", "Legumbres", "Enlatados", "Harina"));
            catalogo.put("Textil", java.util.List.of("Ropa de abrigo", "Frazadas", "Calzado", "Ropa para bebés"));
            catalogo.put("Salud", java.util.List.of("Medicamentos", "Botiquín", "Insumos descartables"));
            catalogo.put("Higiene", java.util.List.of("Jabón", "Pañales", "Toallas", "Lavandina"));
            catalogo.put("Educación", java.util.List.of("Útiles escolares", "Libros", "Mochilas"));
            catalogo.put("Hogar", java.util.List.of("Muebles", "Electrodomésticos", "Ropa de cama"));
        }
        model.addAttribute("catalogo", catalogo);
    }

    private static boolean tieneRolDonante(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(autoridad -> "ROLE_DONANTE".equals(autoridad.getAuthority()));
    }

    private static String obtenerSub(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            return null;
        }
        String sub = oidcUser.getSubject();
        return sub == null || sub.isBlank() ? null : sub;
    }

    private static String obtenerCorreo(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            return null;
        }
        return oidcUser.getAttribute("email");
    }
}
