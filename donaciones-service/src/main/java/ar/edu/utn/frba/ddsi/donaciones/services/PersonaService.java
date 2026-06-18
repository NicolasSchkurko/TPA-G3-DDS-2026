package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.clients.NotificacionesClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.NotificacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.PersonaDonanteDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.Whatsapp;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.*;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDePersonas;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PersonaService {

  // Inyección de dependencias en lugar de llamar al getInstance()
  private final RepositorioDePersonas repositorio;
  private final NotificacionesClient notificacionClient;

  public PersonaService(RepositorioDePersonas repositorio, NotificacionesClient notificacionClient) {
    this.repositorio = repositorio;
    this.notificacionClient = notificacionClient;
  }

  // --- CRUD METHODS ---

  public PersonaDonanteDTO crearPersona(PersonaDonanteDTO dto) {
    Direccion direccion = mapearDireccionDesdeDTO(dto.getDireccion());

    if ("HUMANA".equalsIgnoreCase(dto.getTipoPersona())) {
      Genero genero = Genero.valueOf(dto.getGenero().toUpperCase());
      Humano humano = new Humano(dto.getNombre(), dto.getApellido(), dto.getEdad(), dto.getNumeroDeDocumento(), genero);

      PersonaHumana nuevaPersona = new PersonaHumana(humano, direccion);
      return mapToDto(repositorio.save(nuevaPersona));

    } else if ("JURIDICA".equalsIgnoreCase(dto.getTipoPersona())) {
      TipoJuridico tipo = TipoJuridico.valueOf(dto.getTipoJuridico().toUpperCase());
      PersonaJuridica nuevaPersona = new PersonaJuridica(
          direccion, dto.getRazonSocial(), dto.getRubro(), tipo, dto.getCuit(), new ArrayList<>()
      );

      return mapToDto(repositorio.save(nuevaPersona));
    } else {
      throw new IllegalArgumentException("Tipo de persona inválido");
    }
  }

  public List<PersonaDonanteDTO> listarTodas() {
    return repositorio.findAll().stream()
                      .map(this::mapToDto)
                      .filter(Objects::nonNull)
                      .collect(Collectors.toList());
  }

  public PersonaDonanteDTO buscarPorId(UUID id) {
    return repositorio.findById(id)
                      .map(this::mapToDto)
                      .orElseThrow(() -> new IllegalArgumentException("No se encontró la persona con ID: " + id));
  }

  public PersonaDonanteDTO buscarPorNombre(String nombreBuscado) {
    return repositorio.findByNombreCompleto(nombreBuscado)
                      .map(this::mapToDto)
                      .orElse(null);
  }

  public PersonaDonanteDTO actualizarPersona(UUID id, PersonaDonanteDTO dto) {
    PersonaDonante existente = repositorio.findById(id)
                                          .orElseThrow(() -> new IllegalArgumentException("No se encontró la persona con ID: " + id));

    Direccion nuevaDireccion = mapearDireccionDesdeDTO(dto.getDireccion());

    if (existente instanceof PersonaHumana ph && "HUMANA".equalsIgnoreCase(dto.getTipoPersona())) {
      ph.getPersona().setNombre(dto.getNombre());
      ph.getPersona().setApellido(dto.getApellido());
      ph.getPersona().setEdad(dto.getEdad());
      ph.setDireccion(nuevaDireccion);
      // Actualizar otros campos según necesidad
    } else if (existente instanceof PersonaJuridica pj && "JURIDICA".equalsIgnoreCase(dto.getTipoPersona())) {
      pj.setRazonSocial(dto.getRazonSocial());
      pj.setCuit(dto.getCuit());
      pj.setDireccion(nuevaDireccion);
      // Actualizar otros campos según necesidad
    } else {
      throw new IllegalArgumentException("El tipo de persona del DTO no coincide con la entidad almacenada o es inválido.");
    }

    return mapToDto(repositorio.save(existente));
  }

  public void eliminarPersona(UUID id) {
    repositorio.deleteById(id);
  }

  // --- GESTIÓN DE MEDIOS DE CONTACTO ---

  public List<MediosContactoDTO> obtenerMediosContacto(UUID id) {
    PersonaDonante persona = repositorio.findById(id)
                                        .orElseThrow(() -> new IllegalArgumentException("No se encontró la persona con ID: " + id));

    return mapMediosContactoToDto(persona.getMediosDeContacto());
  }

  public PersonaDonanteDTO agregarMedioContacto(UUID id, MediosContactoDTO dto) {
    PersonaDonante persona = repositorio.findById(id)
                                        .orElseThrow(() -> new IllegalArgumentException("No se encontró la persona con ID: " + id));

    MedioDeContacto nuevoMedio;
    switch (dto.getTipo().toUpperCase()) {
      case "EMAIL":
        nuevoMedio = new Mail(dto.getValor());
        break;
      case "TELEFONO":
        nuevoMedio = new Telefono(dto.getValor());
        break;
      case "WHATSAPP":
        nuevoMedio = new Whatsapp(dto.getValor());
        break;
      default:
        throw new IllegalArgumentException("Tipo de medio de contacto no soportado. Use EMAIL, TELEFONO o WHATSAPP.");
    }

    persona.agregarMedioDeContacto(nuevoMedio);
    return mapToDto(repositorio.save(persona));
  }

  // --- AUTOMATION ---

  @Scheduled(cron = "0 0 0 * * ?") // una vez por día
  public Void revisarActividadesPersonas() {
    List<PersonaDonante> personas = repositorio.findAll();
    for (PersonaDonante persona : personas) {
      revisarActividadPerfil(persona);
    }
    return null;
  }

  private void revisarActividadPerfil(PersonaDonante persona) {
    // Validamos que tenga formularios antes de chequear inactividad
    if (persona.getFormularios() != null && !persona.getFormularios().isEmpty()) {
      if (persona.getFormularios().getLast().getFechaRealizacion().plusDays(20).isBefore(LocalDate.now())) {

        MedioDeContacto medioPredeterminado = null;
        if (persona.getMediosDeContacto() != null) {
          medioPredeterminado = persona.getMediosDeContacto().getMedioDeContactoPredeterminado();
        }

        MediosContactoDTO medioDTO = null;
        if (medioPredeterminado != null) {
          String tipo;
          if (medioPredeterminado instanceof Mail) {
            tipo = "EMAIL";
          } else if (medioPredeterminado instanceof Whatsapp) {
            tipo = "WHATSAPP";
          } else if (medioPredeterminado instanceof Telefono) {
            tipo = "TELEFONO";
          } else {
            tipo = medioPredeterminado.getClass().getSimpleName().toUpperCase();
          }
          medioDTO = new MediosContactoDTO(tipo, medioPredeterminado.getValor());
        }

        NotificacionDTO notificacion = new NotificacionDTO(
            medioDTO,
            medioPredeterminado != null ? medioPredeterminado.getValor() : null,
            "¡Te extrañamos! Hace más de 20 días que no registras actividad. Tu ayuda es muy valiosa.",
            "Inactividad del perfil"
        );

        notificacionClient.enviarNotificacion(notificacion);
      }
    }
  }

  // --- MAPPER ---

  private PersonaDonanteDTO mapToDto(PersonaDonante entidad) {
    if (entidad == null) {
      return null;
    }

    PersonaDonanteDTO responseDTO = new PersonaDonanteDTO();
    responseDTO.setId(entidad.getId());

    try {
      responseDTO.setNombreAMostrar(entidad.darNombre());
    } catch (Exception e) {
      responseDTO.setNombreAMostrar(null);
    }

    Direccion direccion = null;

    if (entidad instanceof PersonaHumana ph) {
      responseDTO.setTipoPersona("HUMANA");
      if (ph.getPersona() != null) {
        responseDTO.setNombre(ph.getPersona().getNombre());
        responseDTO.setApellido(ph.getPersona().getApellido());
        responseDTO.setEdad(ph.getPersona().getEdad());
        responseDTO.setNumeroDeDocumento(ph.getPersona().getNumeroDeDocumento());
        responseDTO.setGenero(ph.getPersona().getGenero() != null ? ph.getPersona().getGenero().name() : null);
      }
      direccion = ph.getDireccion();
    } else if (entidad instanceof PersonaJuridica pj) {
      responseDTO.setTipoPersona("JURIDICA");
      responseDTO.setRazonSocial(pj.getRazonSocial());
      responseDTO.setCuit(pj.getCuit());
      responseDTO.setRubro(pj.getRubro());
      responseDTO.setTipoJuridico(pj.getTipoJuridico() != null ? pj.getTipoJuridico().name() : null);
      direccion = pj.getDireccion();
    } else {
      responseDTO.setTipoPersona("DESCONOCIDO");
    }

    // Mapeo de dirección
    if (direccion != null) {
      DireccionDTO dirDto = new DireccionDTO();
      dirDto.setCalleUno(direccion.getCalleUno());
      dirDto.setCalleDos(direccion.getCalleDos());
      dirDto.setAltura(direccion.getAltura());
      dirDto.setPiso(direccion.getPiso());
      dirDto.setDepartamento(direccion.getDepartamento());
      if (direccion.getCiudad() != null) {
        dirDto.setCiudad(direccion.getCiudad().getNombre());
        if (direccion.getCiudad().getProvincia() != null) {
          dirDto.setProvincia(direccion.getCiudad().getProvincia().getNombre());
          if (direccion.getCiudad().getProvincia().getPais() != null) {
            dirDto.setPais(direccion.getCiudad().getProvincia().getPais().getNombre());
          }
        }
      }
      responseDTO.setDireccion(dirDto);
    }

    // Usamos el nuevo método de mapeo reutilizable
    responseDTO.setMediosDeContacto(mapMediosContactoToDto(entidad.getMediosDeContacto()));

    return responseDTO;
  }

  // Método auxiliar para no duplicar la lógica de mapeo de contactos
  private List<MediosContactoDTO> mapMediosContactoToDto(MediosDeContacto mediosDeContacto) {
    if (mediosDeContacto != null && mediosDeContacto.getListaMediosDeContacto() != null) {
      return mediosDeContacto.getListaMediosDeContacto().stream()
                             .map(medio -> {
                               String tipo;
                               if (medio instanceof Mail) {
                                 tipo = "EMAIL";
                               } else if (medio instanceof Whatsapp) {
                                 tipo = "WHATSAPP";
                               } else if (medio instanceof Telefono) {
                                 tipo = "TELEFONO";
                               } else {
                                 tipo = "DESCONOCIDO";
                               }
                               return new MediosContactoDTO(tipo, medio.getValor());
                             })
                             .collect(Collectors.toList());
    }
    return new ArrayList<>();
  }

  private Direccion mapearDireccionDesdeDTO(DireccionDTO dto) {
    if (dto == null) {
      return null;
    }
    Pais pais = new Pais(dto.getPais());
    Provincia provincia = new Provincia(dto.getProvincia(), pais);
    Ciudad ciudad = new Ciudad(dto.getCiudad(), provincia);

    return new Direccion(
        dto.getCalleUno(),
        dto.getCalleDos(),
        dto.getAltura(),
        dto.getPiso(),
        dto.getDepartamento(),
        ciudad
    );
  }
}