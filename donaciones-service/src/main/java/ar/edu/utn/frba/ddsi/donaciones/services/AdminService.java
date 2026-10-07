package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.admin.AdminDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioAdministradores;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioPersonas;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final RepositorioAdministradores repositorioAdministradores;
    private final RepositorioPersonas repositorioPersonas;

    public AdminService(RepositorioAdministradores repositorioAdministradores, RepositorioPersonas repositorioPersonas) {
        this.repositorioAdministradores = repositorioAdministradores;
        this.repositorioPersonas = repositorioPersonas;
    }

    public List<AdminDTO> getAdmins() {
        return repositorioAdministradores.obtenerTodos().stream()
                .map(AdminDTO::from)
                .collect(Collectors.toList());
    }

public AdminDTO getAdminPorId(UUID id) {
        // Sin .get(): sobre un Optional vacio eso tira NoSuchElementException y el endpoint
        // devolvia 500 en vez del 404 que el controller ya sabe armar.
        //
        // El `if (admin == null)` que venia despues era codigo muerto: Optional.get() nunca
        // devuelve null, o trae el valor o revienta antes de llegar ahi. Por eso el mensaje de
        // "No se encontro el administrador" nunca llego a imprimirse nunca.
        //
        // Y no es un caso teorico: es el endpoint que consulta incentivos-service para saber si
        // un usuario es administrador (DonacionClient). Un id que no existe le devolvia 500.
        Administrador admin = repositorioAdministradores.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontro el administrador con ID: " + id));
        return AdminDTO.from(admin);
    }

    public AdminDTO crearAdministrador(AdminDTO dto) {
        Administrador nuevoAdmin = dto.toDomain();
        // La Humana vive en su propio repositorio (RepositorioPersonas), igual que Juridica
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

    public AdminDTO actualizarAdmin(UUID id, AdminDTO dto) {
        // Mismo motivo que en getAdminPorId: .get() sobre un Optional vacio es una
        // excepcion, y el null check de abajo no se ejecutaba nunca.
        Administrador existente = repositorioAdministradores.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontro la persona con ID: " + id));

        Administrador datosNuevos = dto.toDomain();
        if (existente.getHumano() != null && datosNuevos.getHumano() != null) {
            repositorioPersonas.modificarPersona(existente.getHumano().getId(), datosNuevos.getHumano());
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
