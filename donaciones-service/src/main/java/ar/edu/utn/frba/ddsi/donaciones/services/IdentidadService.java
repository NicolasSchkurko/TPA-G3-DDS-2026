package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.identidad.IdentidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeAdministradores;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeDonantes;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeEntidadesBeneficiarias;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resuelve qué perfiles de dominio están vinculados a una identidad.
 *
 * <p>La identidad estable es el {@code sub} de Keycloak: es inmutable aunque cambie el
 * email o el username. El email se mantiene como medio de contacto y como fallback
 * transitorio para cuentas creadas antes de que existiera el vínculo.</p>
 */
@Service
public class IdentidadService {

    private static final java.util.Set<String> ROLES_VALIDOS = java.util.Set.of("DONANTE", "ENTIDAD", "ADMIN");

    private final RepositorioDeEntidadesBeneficiarias repositorioEntidades;
    private final RepositorioDeDonantes repositorioDonantes;
    private final RepositorioDeAdministradores repositorioAdministradores;

    public IdentidadService(RepositorioDeEntidadesBeneficiarias repositorioEntidades,
                            RepositorioDeDonantes repositorioDonantes,
                            RepositorioDeAdministradores repositorioAdministradores) {
        this.repositorioEntidades = repositorioEntidades;
        this.repositorioDonantes = repositorioDonantes;
        this.repositorioAdministradores = repositorioAdministradores;
    }

    /** Identifica los perfiles vinculados al email indicado. */
    public IdentidadDTO porEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio.");
        }
        String normalizado = email.trim().toLowerCase();

        IdentidadDTO dto = new IdentidadDTO();
        dto.setEmail(normalizado);

        repositorioEntidades.buscarPorEmailContacto(normalizado)
                .ifPresent(entidad -> dto.setEntidadId(entidad.getId()));
        repositorioDonantes.buscarPorEmailContacto(normalizado)
                .ifPresent(donante -> dto.setDonanteId(donante.getId()));
        repositorioAdministradores.buscarPorEmailContacto(normalizado)
                .ifPresent(admin -> dto.setAdminId(admin.getId()));

        completarRoles(dto);
        return dto;
    }

    /** Identifica los perfiles vinculados a la cuenta de Keycloak indicada. */
    public IdentidadDTO porSub(String sub) {
        String normalizado = validarSub(sub);

        IdentidadDTO dto = new IdentidadDTO();
        repositorioEntidades.buscarPorKeycloakSub(normalizado)
                .ifPresent(entidad -> dto.setEntidadId(entidad.getId()));
        repositorioDonantes.buscarPorKeycloakSub(normalizado)
                .ifPresent(donante -> dto.setDonanteId(donante.getId()));
        repositorioAdministradores.buscarPorKeycloakSub(normalizado)
                .ifPresent(admin -> dto.setAdminId(admin.getId()));

        completarRoles(dto);
        return dto;
    }

    /**
     * Vincula la cuenta de Keycloak indicada con el perfil de dominio indicado.
     * Idempotente: si ya están vinculados, no hace nada.
     */
    public void vincular(String sub, String rol, UUID perfilId) {
        String keycloakSub = validarSub(sub);
        if (perfilId == null) {
            throw new IllegalArgumentException("El perfilId es obligatorio.");
        }
        String rolNormalizado = rol == null ? "" : rol.trim().toUpperCase();
        if (!ROLES_VALIDOS.contains(rolNormalizado)) {
            throw new IllegalArgumentException("El rol debe ser DONANTE, ENTIDAD o ADMIN.");
        }

        switch (rolNormalizado) {
            case "DONANTE" -> {
                Donante perfil = repositorioDonantes.buscarPorId(perfilId)
                        .orElseThrow(() -> new EntityNotFoundException("No se encontró el donante con ID: " + perfilId));
                verificarVinculo(perfil.getKeycloakSub(), keycloakSub,
                        repositorioDonantes.buscarPorKeycloakSub(keycloakSub).map(Donante::getId), perfilId);
                perfil.setKeycloakSub(keycloakSub);
                repositorioDonantes.actualizar(perfilId, perfil);
            }
            case "ENTIDAD" -> {
                EntidadBeneficiaria perfil = repositorioEntidades.buscarPorId(perfilId)
                        .orElseThrow(() -> new EntityNotFoundException("No se encontró la entidad con ID: " + perfilId));
                verificarVinculo(perfil.getKeycloakSub(), keycloakSub,
                        repositorioEntidades.buscarPorKeycloakSub(keycloakSub).map(EntidadBeneficiaria::getId), perfilId);
                perfil.setKeycloakSub(keycloakSub);
                repositorioEntidades.actualizar(perfilId, perfil);
            }
            default -> {
                Administrador perfil = repositorioAdministradores.buscarPorId(perfilId)
                        .orElseThrow(() -> new EntityNotFoundException("No se encontró el administrador con ID: " + perfilId));
                verificarVinculo(perfil.getKeycloakSub(), keycloakSub,
                        repositorioAdministradores.buscarPorKeycloakSub(keycloakSub).map(Administrador::getId), perfilId);
                perfil.setKeycloakSub(keycloakSub);
                repositorioAdministradores.actualizar(perfilId, perfil);
            }
        }
    }

    /**
     * Reglas: un perfil no puede estar vinculado a dos cuentas, y una cuenta no puede
     * estar vinculada a dos perfiles del mismo rol.
     */
    private void verificarVinculo(String subActual, String keycloakSub, Optional<UUID> vinculadoA, UUID perfilId) {
        if (subActual != null && !subActual.isBlank()) {
            if (!subActual.equals(keycloakSub)) {
                throw new IllegalStateException("Ese perfil ya está vinculado a otra cuenta.");
            }
            return; // ya vinculado con este mismo sub: idempotente
        }
        vinculadoA.ifPresent(otro -> {
            if (!otro.equals(perfilId)) {
                throw new IllegalStateException("Esa cuenta ya está vinculada a otro perfil del mismo rol.");
            }
        });
    }

    private static void completarRoles(IdentidadDTO dto) {
        List<String> roles = new java.util.ArrayList<>();
        if (dto.getEntidadId() != null) roles.add("ENTIDAD");
        if (dto.getDonanteId() != null) roles.add("DONANTE");
        if (dto.getAdminId() != null) roles.add("ADMIN");
        dto.setRoles(roles);
    }

    private static String validarSub(String sub) {
        if (sub == null || sub.isBlank()) {
            throw new IllegalArgumentException("El identificador de la cuenta es obligatorio.");
        }
        String normalizado = sub.trim();
        try {
            UUID.fromString(normalizado);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El identificador de la cuenta no es válido.");
        }
        return normalizado;
    }
}
