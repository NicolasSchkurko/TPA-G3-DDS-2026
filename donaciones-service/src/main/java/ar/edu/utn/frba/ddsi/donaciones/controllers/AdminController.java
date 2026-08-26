package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.admin.AdminDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Whatsapp;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.services.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admins")
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> registrarAdmin(@RequestBody AdminDTO dto) {
        try {
            Administrador dominio = mapearADominio(dto);
            Administrador adminCreado = service.crearAdministrador(dominio);
            return new ResponseEntity<>(mapearADto(adminCreado), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarAdmin(@PathVariable UUID id, @RequestBody AdminDTO dto) {
        try {
            Administrador dominio = mapearADominio(dto);
            Administrador actualizada = service.actualizarAdmin(id, dominio);
            return ResponseEntity.ok(mapearADto(actualizada));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarAdmin(@PathVariable UUID id) {
        service.eliminarAdmin(id);
        return ResponseEntity.noContent().build();
    }

    private Administrador mapearADominio(AdminDTO dto) {
        Genero genero = Genero.valueOf(dto.getGenero().toUpperCase());
        MedioDeContacto contacto = crearMedioDeContacto(dto.getMedioDeContacto().getTipo(), dto.getMedioDeContacto().getValor());

        Humana humano = new Humana(dto.getNombre(), dto.getApellido(), dto.getEdad(), dto.getNumeroDeDocumento(), genero, dto.getNombre());

        return new Administrador(dto.getId(), humano, contacto,dto.getNombreAMostrar());
    }

    private AdminDTO mapearADto(Administrador admin) {
        if (admin == null) {
            return null;
        }

        AdminDTO responseDTO = new AdminDTO();
        responseDTO.setId(admin.getId());

        try {
            responseDTO.setNombreAMostrar(admin.getNombreAMostrar());
        } catch (Exception e) {
            responseDTO.setNombreAMostrar(null);
        }

        if (admin.getHumano() != null) {
            responseDTO.setNombre(admin.getHumano().getNombre());
            responseDTO.setApellido(admin.getHumano().getApellido());
            responseDTO.setEdad(admin.getHumano().getEdad());
            responseDTO.setNumeroDeDocumento(admin.getHumano().getNumeroDeDocumento());
            responseDTO.setGenero(admin.getHumano().getGenero() != null ? admin.getHumano().getGenero().name() : null);
        }

        MediosContactoDTO medio = new MediosContactoDTO(admin.getMedioDeContacto().getTipo(),
                                                        admin.getMedioDeContacto().getValor());
        responseDTO.setMedioDeContacto(medio);

        return responseDTO;
    }

    private MedioDeContacto crearMedioDeContacto(String tipo, String valor) {
        return switch (tipo.toUpperCase()) {
            case "EMAIL" -> new Mail(valor);
            case "TELEFONO" -> new Telefono(valor);
            case "WHATSAPP" -> new Whatsapp(valor);
            default -> throw new IllegalArgumentException("Tipo de medio de contacto no soportado. Use EMAIL, TELEFONO o WHATSAPP.");
        };
    }

}