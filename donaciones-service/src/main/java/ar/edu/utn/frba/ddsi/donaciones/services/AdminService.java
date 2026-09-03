package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.admin.AdminDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorPersonas;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioAdministradores;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final RepositorioAdministradores repositorioAdministradores;
    private final GestorPersonas gestorPersonas;

    public AdminService(RepositorioAdministradores repositorioAdministradores, GestorPersonas gestorPersonas) {
        this.repositorioAdministradores = repositorioAdministradores;
        this.gestorPersonas = gestorPersonas;
    }

    public List<AdminDTO> getAdmins() {
        return repositorioAdministradores.obtenerTodos().stream()
                .map(AdminDTO::from)
                .collect(Collectors.toList());
    }

    public AdminDTO getAdminPorId(UUID id) {
        Administrador admin = repositorioAdministradores.buscarPorId(id).get();
        if (admin == null) {
            throw new IllegalArgumentException("No se encontró el administrador con ID: " + id);
        }
        return AdminDTO.from(admin);
    }

    public AdminDTO crearAdministrador(AdminDTO dto) {
        Administrador nuevoAdmin = dto.toDomain();
        // La Humana vive en su propio repositorio (RepositorioPersonas), igual que Juridica
        // para EntidadBeneficiaria: se registra explícitamente antes de guardar el Administrador.
        if (nuevoAdmin.getHumano() != null) gestorPersonas.registrarPersona(nuevoAdmin.getHumano());
        try {
            repositorioAdministradores.guardar(nuevoAdmin);
            System.out.println("Administrador registrado con éxito con ID: " + nuevoAdmin.getId());
        } catch (IllegalArgumentException e) {
            System.err.println("Error al registrar administrador: " + e.getMessage());
        }
        return AdminDTO.from(nuevoAdmin);
    }

    public AdminDTO actualizarAdmin(UUID id, AdminDTO dto) {
        Administrador existente = repositorioAdministradores.buscarPorId(id).get();
        if (existente == null) throw new IllegalArgumentException("No se encontró la persona con ID: " + id);

        Administrador datosNuevos = dto.toDomain();
        if (existente.getHumano() != null && datosNuevos.getHumano() != null) {
            gestorPersonas.modificarPersona(existente.getHumano().getId(), datosNuevos.getHumano());
        }
        existente.setHumano(datosNuevos.getHumano());
        existente.setMedioDeContacto(datosNuevos.getContacto());
        existente.setNombreAMostrar(datosNuevos.getNombreAMostrar());

        try {
            repositorioAdministradores.actualizar(id, datosNuevos);
            System.out.println("Administrador actualizado con éxito.");
        } catch (IllegalArgumentException e) {
            System.err.println("Error al modificar administrador: " + e.getMessage());
        }
        return AdminDTO.from(existente);
    }

    public void eliminarAdmin(UUID id) {
        repositorioAdministradores.eliminarPorId(id);
        System.out.println("Administrador dado de baja (si existía).");
    }

}