package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.admin.AdminDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Whatsapp;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.*;

import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioAdministradores;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService {
    private final RepositorioAdministradores repositorio;

    public AdminService(RepositorioAdministradores repositorio) {
        this.repositorio = repositorio;
    }

    public AdminDTO crearAdministrador(AdminDTO dto) {
        Genero genero = Genero.valueOf(dto.getGenero().toUpperCase());
        MedioDeContacto contacto = crearMedioDeContacto(dto.getMedioDeContacto().getTipo(), dto.getMedioDeContacto().getValor());

        Administrador nuevoAdmin = new Administrador(dto.getNombreAMostrar(),
                                                     dto.getNombre(), dto.getApellido(), dto.getEdad(),
                                                     dto.getNumeroDeDocumento(), genero.name(), contacto);

        return mapToDto(repositorio.save(nuevoAdmin));
    }

    public AdminDTO actualizarAdmin(UUID id, AdminDTO dto) {
        Administrador existente = repositorio.findById(id)
                                             .orElseThrow(() -> new IllegalArgumentException("No se encontró la persona con ID: " + id));

        Genero genero = Genero.valueOf(dto.getGenero().toUpperCase());
        MedioDeContacto contacto = crearMedioDeContacto(dto.getMedioDeContacto().getTipo(), dto.getMedioDeContacto().getValor());

        existente.setNombre(dto.getNombre());
        existente.setApellido(dto.getApellido());
        existente.setEdad(dto.getEdad());
        existente.setGenero(genero.name());
        existente.setMedioDeContacto(contacto);

        return mapToDto(repositorio.save(existente));
    }

    public void eliminarAdmin(UUID id) {
        repositorio.deleteById(id);
    }

    /**
     * Medios de contacto de todos los administradores, para uso del
     * sistema de notificaciones (ej. entregas no recibidas).
     */
    public List<MedioDeContacto> obtenerContactosAdministradores() {
        return repositorio.findAll().stream()
                          .map(Administrador::getMedioDeContacto)
                          .toList();
    }

    private MedioDeContacto crearMedioDeContacto(String tipo, String valor) {
        return switch (tipo.toUpperCase()) {
            case "EMAIL" -> new Mail(valor);
            case "TELEFONO" -> new Telefono(valor);
            case "WHATSAPP" -> new Whatsapp(valor);
            default -> throw new IllegalArgumentException("Tipo de medio de contacto no soportado. Use EMAIL, TELEFONO o WHATSAPP.");
        };
    }

    private AdminDTO mapToDto(Administrador admin) {
        if (admin == null) {
            return null;
        }

        AdminDTO responseDTO = new AdminDTO();
        responseDTO.setId(admin.getId());

        try {
            responseDTO.setNombreAMostrar(admin.darNombre());
        } catch (Exception e) {
            responseDTO.setNombreAMostrar(null);
        }

        responseDTO.setNombre(admin.getNombre());
        responseDTO.setApellido(admin.getApellido());
        responseDTO.setEdad(admin.getEdad());
        responseDTO.setNumeroDeDocumento(admin.getNumeroDeDocumento());
        responseDTO.setGenero(admin.getGenero());

        MediosContactoDTO medio = new MediosContactoDTO(admin.getMedioDeContacto().getTipo(),
                                                        admin.getMedioDeContacto().getValor());
        responseDTO.setMedioDeContacto(medio);

        return responseDTO;
    }
}