package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.identidad.IdentidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeAdministradores;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeDonantes;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeEntidadesBeneficiarias;
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

        repositorioEntidades.buscarPorEmailContacto(normalizado).stream().findFirst()
                .ifPresent(entidad -> dto.setEntidadId(entidad.getId()));
        repositorioDonantes.buscarPorEmailContacto(normalizado).stream().findFirst()
                .ifPresent(donante -> dto.setDonanteId(donante.getId()));
        repositorioAdministradores.buscarPorEmailContacto(normalizado).stream().findFirst()
                .ifPresent(admin -> dto.setAdminId(admin.getId()));

        List<String> roles = new java.util.ArrayList<>();
        if (dto.getEntidadId() != null) roles.add("ENTIDAD");
        if (dto.getDonanteId() != null) roles.add("DONANTE");
        if (dto.getAdminId() != null) roles.add("ADMIN");
        dto.setRoles(roles);

        return dto;
    }
}
