package ar.edu.utn.frba.ddsi.front.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cambia el rol activo de la sesión (tabs de la cabecera multi-rol).
 * Solo acepta roles que el usuario realmente tiene; si no, redirige con aviso.
 */
@Controller
@RequestMapping("/rol")
public class RolController {

    @PostMapping("/{rol}")
    public String cambiar(@PathVariable String rol, HttpSession session, Authentication authentication, Model model) {
        String solicitado = rol.toUpperCase();
        if (!esRolSoportado(solicitado) || !tieneRol(authentication, solicitado)) {
            model.addAttribute("error", "No tenés el rol " + solicitado + " asignado. Contactá a una persona administradora.");
            return "sin-acceso";
        }
        session.setAttribute("rolActivo", solicitado);
        return "redirect:" + destinoDe(solicitado);
    }

    private static boolean esRolSoportado(String rol) {
        return switch (rol) {
            case "ADMIN", "DONANTE", "CHOFER", "ENTIDAD" -> true;
            default -> false;
        };
    }

    private static boolean tieneRol(Authentication authentication, String rol) {
        if (authentication == null) return false;
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return roles.contains("ROLE_" + rol);
    }

    private static String destinoDe(String rol) {
        return switch (rol) {
            case "ADMIN" -> "/panel";
            case "DONANTE" -> "/donante";
            case "CHOFER" -> "/chofer";
            default -> "/entidad";
        };
    }
}
