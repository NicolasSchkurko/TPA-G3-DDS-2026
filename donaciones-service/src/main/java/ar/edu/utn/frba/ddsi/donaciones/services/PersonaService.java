package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.clients.NotificacionesClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.IDDTO;
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
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.LectorCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.MapeoCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter.PersonaDonanteFilaConverter;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDePersonas;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PersonaService {

  // Inyección de dependencias en lugar de llamar al getInstance()
  private final RepositorioDePersonas repositorio;
  private final NotificacionesClient notificacionClient;
  private final IncentivosClient incentivosClient;

  public PersonaService(RepositorioDePersonas repositorio, NotificacionesClient notificacionClient, IncentivosClient incentivosClient) {
    this.repositorio = repositorio;
    this.notificacionClient = notificacionClient;
    this.incentivosClient=incentivosClient;
  }

  // --- CRUD METHODS ---

  public PersonaDonanteDTO crearPersona(PersonaDonanteDTO dto) {
    Direccion direccion = mapearDireccionDesdeDTO(dto.getDireccion());

    if ("HUMANA".equalsIgnoreCase(dto.getTipoPersona())) {
      Genero genero = Genero.valueOf(dto.getGenero().toUpperCase());
      Humano humano = new Humano(dto.getNombre(), dto.getApellido(), dto.getEdad(), dto.getNumeroDeDocumento(), genero);

      PersonaHumana nuevaPersona = new PersonaHumana(humano, direccion, dto.getNombreAMostrar());
      IDDTO peticion = new IDDTO(
          nuevaPersona.getId(),
          nuevaPersona.getNombreDeUsuario()
          );

      incentivosClient.peticionCrearPerfil(peticion);
      return mapToDto(repositorio.save(nuevaPersona));

    } else if ("JURIDICA".equalsIgnoreCase(dto.getTipoPersona())) {
      TipoJuridico tipo = TipoJuridico.valueOf(dto.getTipoJuridico().toUpperCase());
      PersonaJuridica nuevaPersona = new PersonaJuridica(
          direccion, dto.getRazonSocial(), dto.getRubro(), tipo, dto.getCuit(), new ArrayList<>(), dto.getNombreAMostrar()
      );
      IDDTO peticion = new IDDTO(
          nuevaPersona.getId(),
          nuevaPersona.getNombreDeUsuario()
      );

      incentivosClient.peticionCrearPerfil(peticion);
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

  public String importarDonantes(MultipartFile file, List<MapeoCSV> mapeosCsv) {
    try {
      // 1. Extraemos los bytes del archivo para poder usarlo dentro del hilo asincrónico,
      byte[] fileBytes = file.getBytes();

      // 2. Disparamos un hilo asincrónico para no bloquear al usuario
      CompletableFuture.runAsync(() -> {
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
          // Instanciamos el conversor y el lector de forma temporal para esta única importación
          PersonaDonanteFilaConverter conversor = new PersonaDonanteFilaConverter(mapeosCsv);
          LectorCSV<PersonaDonante> lectorTemporal = new LectorCSV<>(',', conversor);

          // El LectorCSV internamente hace los loggers (warnings) de las filas que fallan
          List<PersonaDonante> donantesImportados = lectorTemporal.importar(inputStream);

          int guardados = 0;
          for (PersonaDonante donante : donantesImportados) {
            try {
              repositorio.save(donante);
              guardados++;
            } catch (Exception e) {
              System.err.println("Fallo al persistir un donante del CSV: " + e.getMessage());
            }
          }

          System.out.println("Importación CSV finalizada en 2do plano. Se procesaron y guardaron " + guardados + " donantes.");

        } catch (IOException e) {
          System.err.println("Error de IO al leer el stream del CSV: " + e.getMessage());
        }
      });

      // 3. Retornamos un string rápidamente mientras el hilo sigue trabajando
      return "El archivo fue recibido con éxito. La importación se está ejecutando en segundo plano y los errores de formato quedarán en los logs del servidor.";

    } catch (IOException e) {
      throw new RuntimeException("Error al acceder al archivo CSV", e);
    }
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

    responseDTO.setMediosDeContacto(mapMediosContactoToDto(entidad.getMediosDeContacto()));

    return responseDTO;
  }

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
