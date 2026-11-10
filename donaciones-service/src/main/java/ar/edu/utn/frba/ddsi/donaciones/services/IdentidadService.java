package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.identidad.IdentidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioAdministradores;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioDonantes;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioEntidadesBeneficiarias;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Resuelve qué perfiles de dominio están vinculados a un correo electrónico.
 * Se usa para que una misma persona pueda operar como donante, entidad o
 * administrador desde una única cuenta, según los roles habilitados en el front.
 */
@Service
public class IdentidadService {

    private final RepositorioEntidadesBeneficiarias repositorioEntidades;
    private final RepositorioDonantes repositorioDonantes;
    private final RepositorioAdministradores repositorioAdministradores;

    public IdentidadService(RepositorioEntidadesBeneficiarias repositorioEntidades,
                            RepositorioDonantes repositorioDonantes,
                            RepositorioAdministradores repositorioAdministradores) {
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

        List<String> roles = new java.util.ArrayList<>();
        if (dto.getEntidadId() != null) roles.add("ENTIDAD");
        if (dto.getDonanteId() != null) roles.add("DONANTE");
        if (dto.getAdminId() != null) roles.add("ADMIN");
        dto.setRoles(roles);

        return dto;
    }
}
