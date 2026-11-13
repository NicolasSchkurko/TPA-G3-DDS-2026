package ar.edu.utn.frba.ddsi.donaciones.services;

import jakarta.persistence.EntityNotFoundException;
import ar.edu.utn.frba.ddsi.donaciones.dto.admin.AdminDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeAdministradores;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDePersonas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final RepositorioDeAdministradores repositorioAdministradores;
    private final RepositorioDePersonas repositorioPersonas;

    public AdminService(RepositorioDeAdministradores repositorioAdministradores,
                        RepositorioDePersonas repositorioPersonas) {
        this.repositorioAdministradores = repositorioAdministradores;
        this.repositorioPersonas = repositorioPersonas;
    }

    @Transactional(readOnly = true)
    public List<AdminDTO> getAdmins() {
        return repositorioAdministradores.obtenerTodos().stream()
                .map(AdminDTO::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminDTO getAdminPorId(UUID id) {
        Administrador admin = repositorioAdministradores.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró el administrador con ID: " + id));
        return AdminDTO.from(admin);
    }

    public AdminDTO crearAdministrador(AdminDTO dto) {
        if (dto.getId() == null || dto.getId().isBlank()) {
            throw new IllegalArgumentException(
                    "El administrador necesita el id de su cuenta de Keycloak.");
        }
        Administrador nuevoAdmin = dto.toDomain();
        if (dto.getKeycloakSub() != null && !dto.getKeycloakSub().isBlank()) {
            String sub = dto.getKeycloakSub().trim();
            repositorioAdministradores.buscarPorKeycloakSub(sub)
                    .ifPresent(a -> { throw new IllegalArgumentException(
                        "Esa cuenta ya está vinculada a un administrador. Iniciá sesión y agregá el rol desde tu cuenta."); });
            nuevoAdmin.setKeycloakSub(sub);
        }
        // La Humana vive en su propio repositorio (RepositorioDePersonas), igual que Juridica
        // para EntidadBeneficiaria: se registra explícitamente antes de guardar el Administrador.
        if (nuevoAdmin.getHumano() != null) repositorioPersonas.registrarPersona(nuevoAdmin.getHumano());
        try {
            repositorioAdministradores.guardar(nuevoAdmin);
            System.out.println("Administrador registrado con éxito con ID: " + nuevoAdmin.getId());
        } catch (IllegalArgumentException e) {
            System.err.println("Error al registrar administrador: " + e.getMessage());
        }
        return AdminDTO.from(nuevoAdmin);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminDTO actualizarAdmin(UUID id, AdminDTO dto) {
        Administrador datosNuevos = dto.toDomain();
        Administrador existente = repositorioAdministradores.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró el administrador con ID: " + id));

        if (existente.getHumano() != null && datosNuevos.getHumano() != null) {
            repositorioPersonas.modificarPersona(existente.getHumano().getId(), datosNuevos.getHumano());
        } else if (datosNuevos.getHumano() != null) {
            repositorioPersonas.registrarPersona(datosNuevos.getHumano());
            existente.setHumano(datosNuevos.getHumano());
        }
        existente.setMedioDeContacto(datosNuevos.getContacto());
        existente.setNombreAMostrar(datosNuevos.getNombreAMostrar());

        repositorioAdministradores.actualizar(id, existente);
        return AdminDTO.from(existente);
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminarAdmin(UUID id) {
        repositorioAdministradores.eliminarPorId(id);
    }
}
