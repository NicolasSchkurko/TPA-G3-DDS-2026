package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.admin.AdminDTO;

import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.EstadoEntrega;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.RutaEnProceso;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Whatsapp;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.*;

import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioAdministradores;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioRutasActivas;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService {
    private final RepositorioRutasActivas repositorioRutasActivas;
    private final RepositorioDonaciones repositorioDonaciones;
    private final RepositorioAdministradores repositorio;

    public AdminService(RepositorioRutasActivas repositorioRutasActivas,
                        RepositorioDonaciones repositorioDonaciones,
                        RepositorioAdministradores repositorio) {
        this.repositorioRutasActivas = repositorioRutasActivas;
        this.repositorioDonaciones = repositorioDonaciones;
        this.repositorio = repositorio;
    }

    public AdminDTO crearAdministrador(AdminDTO dto) {
        Genero genero = Genero.valueOf(dto.getGenero().toUpperCase());

        MedioDeContacto contacto;
        switch (dto.getMedioDeContacto().getTipo().toUpperCase()) {
            case "EMAIL":
                contacto = new Mail(dto.getMedioDeContacto().getValor());
                break;
            case "TELEFONO":
                contacto = new Telefono(dto.getMedioDeContacto().getValor());
                break;
            case "WHATSAPP":
                contacto = new Whatsapp(dto.getMedioDeContacto().getValor());
                break;
            default:
                throw new IllegalArgumentException("Tipo de medio de contacto no soportado. Use EMAIL, TELEFONO o WHATSAPP.");
        }

        Administrador nuevoAdmin = new Administrador(dto.getNombreAMostrar(),
                dto.getNombre(), dto.getApellido(), dto.getEdad(),
                dto.getNumeroDeDocumento(), genero.name(), contacto);

        return mapToDto(repositorio.save(nuevoAdmin));
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
        responseDTO.setGenero(admin.getGenero() != null ? admin.getGenero() : null);

        MediosContactoDTO medio = new MediosContactoDTO(admin.getMedioDeContacto().getTipo(),
                admin.getMedioDeContacto().getValor());
        responseDTO.setMedioDeContacto(medio);

        return responseDTO;
    }

    public AdminDTO actualizarAdmin(UUID id, AdminDTO dto) {
        Administrador existente = repositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la persona con ID: " + id));

        Genero genero = Genero.valueOf(dto.getGenero().toUpperCase());

        MedioDeContacto contacto;
        switch (dto.getMedioDeContacto().getTipo().toUpperCase()) {
            case "EMAIL":
                contacto = new Mail(dto.getMedioDeContacto().getValor());
                break;
            case "TELEFONO":
                contacto = new Telefono(dto.getMedioDeContacto().getValor());
                break;
            case "WHATSAPP":
                contacto = new Whatsapp(dto.getMedioDeContacto().getValor());
                break;
            default:
                throw new IllegalArgumentException("Tipo de medio de contacto no soportado. Use EMAIL, TELEFONO o WHATSAPP.");
        }

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

    public List<RutaEnProceso> obtenerEntregasNoRecibidas() {
        return repositorioRutasActivas.findAll().stream()
                .filter(ruta -> ruta.getEstadoEntrega() == EstadoEntrega.NO_RECIBIDA)
                .toList();
    }

    public void revisarEntregaNoRecibida(UUID idEntrega, String nuevoEstado) {
        RutaEnProceso ruta = repositorioRutasActivas.findByIdEntrega(idEntrega)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entrega con ID: " + idEntrega));

        if (ruta.getEstadoEntrega() != EstadoEntrega.NO_RECIBIDA) {
            throw new IllegalArgumentException("Solo se pueden revisar entregas NO_RECIBIDA");
        }

        EstadoEntrega estadoEntrega = convertirEstadoEntrega(nuevoEstado);
        if (estadoEntrega == EstadoEntrega.PENDIENTE) {
            registrarRegresoADeposito(ruta);
            return;
        }

        //no se como revisara las de replanificar

        ruta.setEstadoEntrega(estadoEntrega);
        repositorioRutasActivas.save(ruta);
    }

    private void registrarRegresoADeposito(RutaEnProceso ruta) {
        List<UUID> idsDonaciones = ruta.getPaquete() != null ? ruta.getPaquete().getIdsDonaciones() : List.of();
        idsDonaciones.stream()
                .map(repositorioDonaciones::findById)
                .flatMap(java.util.Optional::stream)
                .forEach(donacion -> donacion.actualizarEstado(
                        Estado.EN_DEPOSITO,
                        "La donacion regreso al deposito luego de una entrega no recibida"
                ));

        ruta.setEstadoEntrega(EstadoEntrega.PENDIENTE);
        repositorioRutasActivas.save(ruta);
    }

    private EstadoEntrega convertirEstadoEntrega(String estadoEntrega) {
        if (estadoEntrega == null) {
            throw new IllegalArgumentException("El estado de entrega es obligatorio");
        }

        return switch (estadoEntrega.toUpperCase()) {
            case "PENDIENTE" -> EstadoEntrega.PENDIENTE; //podria o no add + casos de revision
//            case "EN_VIAJE", "EN VIAJE" -> EstadoEntrega.EN_VIAJE;
//            case "NO_RECIBIDA", "NO RECIBIDA" -> EstadoEntrega.NO_RECIBIDA;
            default -> throw new IllegalArgumentException("Estado de entrega invalido para revision: " + estadoEntrega);
        };
    }
}
