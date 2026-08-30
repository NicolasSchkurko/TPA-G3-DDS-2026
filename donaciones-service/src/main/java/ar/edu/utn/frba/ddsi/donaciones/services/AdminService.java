package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.admin.AdminDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorAdministradores;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final GestorAdministradores gestorAdministradores;

    public AdminService(GestorAdministradores gestorAdministradores) {
        this.gestorAdministradores = gestorAdministradores;
    }

    public List<AdminDTO> getAdmins() {
        return gestorAdministradores.listarTodosLosAdministradores().stream()
                .map(AdminDTO::from)
                .collect(Collectors.toList());
    }

    public AdminDTO getAdminPorId(UUID id) {
        Administrador admin = gestorAdministradores.obtenerAdministrador(id);
        if (admin == null) {
            throw new IllegalArgumentException("No se encontró el administrador con ID: " + id);
        }
        return AdminDTO.from(admin);
    }

    public AdminDTO crearAdministrador(AdminDTO dto) {
        Administrador nuevoAdmin = dto.toDomain();
        gestorAdministradores.registrarAdministrador(nuevoAdmin);
        return AdminDTO.from(nuevoAdmin);
    }

    public AdminDTO actualizarAdmin(UUID id, AdminDTO dto) {
        Administrador existente = gestorAdministradores.obtenerAdministrador(id);
        if (existente == null) throw new IllegalArgumentException("No se encontró la persona con ID: " + id);

        Administrador datosNuevos = dto.toDomain();
        existente.setHumano(datosNuevos.getHumano());
        existente.setMedioDeContacto(datosNuevos.getContacto());
        existente.setNombreAMostrar(datosNuevos.getNombreAMostrar());

        gestorAdministradores.modificarAdministrador(id, existente);
        return AdminDTO.from(existente);
    }

    public void eliminarAdmin(UUID id) {
        gestorAdministradores.darDeBajaAdministrador(id);
    }

}