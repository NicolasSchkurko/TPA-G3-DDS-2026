package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.admin.AdminDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Whatsapp;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorAdministradores;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    private final GestorAdministradores gestorAdministradores;

    public AdminService(GestorAdministradores gestorAdministradores) {
        this.gestorAdministradores = gestorAdministradores;
    }

    public AdminDTO crearAdministrador(AdminDTO dto) {
        Administrador nuevoAdmin = mapearADominio(dto);
        gestorAdministradores.registrarAdministrador(nuevoAdmin);
        return mapearADto(nuevoAdmin);
    }

    public AdminDTO actualizarAdmin(UUID id, AdminDTO dto) {
        Administrador datosNuevos = mapearADominio(dto);
        Administrador existente = gestorAdministradores.obtenerAdministrador(id);
        if (existente == null) {
            throw new IllegalArgumentException("No se encontró el administrador con ID: " + id);
        }

        existente.setHumano(datosNuevos.getHumano());
        existente.setMedioDeContacto(datosNuevos.getMedioDeContacto());
        existente.setNombreAMostrar(datosNuevos.getNombreAMostrar());

        gestorAdministradores.modificarAdministrador(id, existente);
        return mapearADto(existente);
    }

    public void eliminarAdmin(UUID id) {
        gestorAdministradores.darDeBajaAdministrador(id);
    }

    public List<MedioDeContacto> obtenerContactosAdministradores() {
        return gestorAdministradores.listarTodosLosAdministradores().stream()
                                    .map(Administrador::getContacto)
                                    .toList();
    }

    // --- MAPPERS INTERNOS ---

    private Administrador mapearADominio(AdminDTO dto) {
        Genero genero = dto.getGenero() != null ? Genero.valueOf(dto.getGenero().toUpperCase()) : Genero.OTRO;

        MedioDeContacto contacto = null;
        if (dto.getMedioDeContacto() != null) {
            contacto = crearMedioDeContacto(dto.getMedioDeContacto().getTipo(), dto.getMedioDeContacto().getValor());
        } else {
            throw new IllegalArgumentException("El administrador debe tener un medio de contacto asignado.");
        }

        Humana humano = new Humana(dto.getNombre(), dto.getApellido(), dto.getEdad(), dto.getNumeroDeDocumento(), genero, dto.getNombreAMostrar());
        return new Administrador(dto.getId() != null ? dto.getId() : UUID.randomUUID(), humano, contacto, dto.getNombreAMostrar());
    }

    private AdminDTO mapearADto(Administrador admin) {
        if (admin == null) return null;

        AdminDTO responseDTO = new AdminDTO();
        responseDTO.setId(admin.getId());
        responseDTO.setNombreAMostrar(admin.getNombreAMostrar());

        if (admin.getHumano() != null) {
            responseDTO.setNombre(admin.getHumano().getNombre());
            responseDTO.setApellido(admin.getHumano().getApellido());
            responseDTO.setEdad(admin.getHumano().getEdad());
            responseDTO.setNumeroDeDocumento(admin.getHumano().getNumeroDeDocumento());
            responseDTO.setGenero(admin.getHumano().getGenero() != null ? admin.getHumano().getGenero().name() : null);
        }

        if (admin.getContacto() != null) {
            MediosContactoDTO medio = new MediosContactoDTO(admin.getContacto().getTipo(), admin.getContacto().getValor());
            responseDTO.setMedioDeContacto(medio);
        }

        return responseDTO;
    }

    private MedioDeContacto crearMedioDeContacto(String tipo, String valor) {
        if (tipo == null || valor == null) throw new IllegalArgumentException("Tipo y valor de contacto requeridos.");
        return switch (tipo.toUpperCase()) {
            case "EMAIL" -> new Mail(valor);
            case "TELEFONO" -> new Telefono(valor);
            case "WHATSAPP" -> new Whatsapp(valor);
            default -> throw new IllegalArgumentException("Tipo de medio de contacto no soportado. Use EMAIL, TELEFONO o WHATSAPP.");
        };
    }
}