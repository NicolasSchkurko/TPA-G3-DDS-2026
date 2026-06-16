package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.PersonaDonanteDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.*;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDePersonas;
import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PersonaService {

  // Instancia de tu repositorio actual
  private final RepositorioDePersonas repositorio = RepositorioDePersonas.getInstance();

  private final NotificacionesClient notificacionClient;
  public PersonaService(NotificacionesClient notificacionClient) {
      this.notificacionClient = notificacionClient;
  }

  public void crearPersona(PersonaDonanteDTO dto) {
    if ("HUMANA".equalsIgnoreCase(dto.getTipoPersona())) {

      // Mapeo manual a Humano
      Genero genero = Genero.valueOf(dto.getGenero()
                                        .toUpperCase());
      Humano humano = new Humano(
          dto.getNombre(),
          dto.getApellido(),
          dto.getEdad(),
          dto.getNumeroDeDocumento(),
          genero
      );

      // null en direccion por brevedad del ejemplo
      PersonaHumana nuevaPersona = new PersonaHumana(humano, null);
      repositorio.agregarPersona(nuevaPersona);

    } else if ("JURIDICA".equalsIgnoreCase(dto.getTipoPersona())) {

      // Mapeo manual a PersonaJuridica
      TipoJuridico tipo = TipoJuridico.valueOf(dto.getTipoJuridico()
                                                  .toUpperCase());
      PersonaJuridica nuevaPersona = new PersonaJuridica(
          null, // direccion
          dto.getRazonSocial(),
          dto.getRubro(),
          tipo,
          dto.getCuit(),
          new ArrayList<>() // representantes
      );

      repositorio.agregarPersona(nuevaPersona);

    } else {
      throw new IllegalArgumentException("Tipo de persona inválido");
    }
  }

  public PersonaDonanteDTO buscarPorNombre(String nombreBuscado) {
    PersonaDonante entidad = RepositorioDePersonas.getPersonaPorNombreCompleto(nombreBuscado);
    if (entidad == null) {
      return null;
    }
    return mapToDto(entidad);
  }

  public List<PersonaDonanteDTO> listarTodas() {
    List<PersonaDonante> todas = repositorio.getPersonas();
    if (todas == null || todas.isEmpty()) {
      return new ArrayList<>();
    }

    return todas.stream()
                .map(this::mapToDto)          // mapea entidad -> DTO
                .filter(Objects::nonNull)     // descarta mapeos que devolvieron null
                .collect(Collectors.toList());
  }

  /* Helper privado que encapsula el mapping entidad -> DTO */
  private PersonaDonanteDTO mapToDto(PersonaDonante entidad) {
    if (entidad == null) {
      return null;
    }

    PersonaDonanteDTO responseDTO = new PersonaDonanteDTO();

    // Nombre a mostrar (puede lanzar si darNombre impl. no lo soporta; si quieres, envolver en try/catch)
    try {
      responseDTO.setNombreAMostrar(entidad.darNombre());
    } catch (Exception e) {
      responseDTO.setNombreAMostrar(null);
    }

    if (entidad instanceof PersonaHumana) {
      PersonaHumana ph = (PersonaHumana) entidad;
      responseDTO.setTipoPersona("HUMANA");
      if (ph.getPersona() != null) {
        // defensas por si nombre/apellido son null
        responseDTO.setNombre(ph.getPersona()
                                .getNombre());
        responseDTO.setApellido(ph.getPersona()
                                  .getApellido());
      }
    } else if (entidad instanceof PersonaJuridica) {
      PersonaJuridica pj = (PersonaJuridica) entidad;
      responseDTO.setTipoPersona("JURIDICA");
      responseDTO.setRazonSocial(pj.getRazonSocial());
      responseDTO.setCuit(pj.getCuit());
    } else {
      // Si existen otros subtipos, se pueden manejar aquí; por ahora devolvemos tipo genérico
      responseDTO.setTipoPersona("DESCONOCIDO");
    }

    return responseDTO;
  }
  @Scheduled(cron = "0 0 0 * * ?") // una vez por día
  public Void revisarActividadesPersonas() {

    List<PersonaDonante> personas = repositorio.findAll();

    for (PersonaDonante persona : personas) {
      revisarActividadPerfil(persona);
    }
    return null;
  }
  private void revisarActividadPerfil(PersonaDonante persona){
    if (persona.getFormularios()
        .getLast()
        .getFechaRealizacion()
        .plusDays(20)
        .isBefore(LocalDate.now())) {
      // perdió la racha, reiniciar misión, etc.
      MediosContactoDTO.NotificacionDTO notificacion = new MediosContactoDTO.NotificacionDTO(
          persona.getMediosDeContacto().getMedioDeContactoPredeterminado().//TODO Desde medio de contacto predeterminado deberia poder acceder al tipo de medio como un string, el tipo de medio seria whatsap,mail o telegram.
     // TODO Deberia poder accederse a la direccion de contacto de la persna desde persona.getdirecciondecontacto
      "doná porfa cada segundo que no donas muere un perrito ",
          "Inactividad del perfil"
      );

      notificacionClient.enviarNotificacion(notificacion);
    }
  }
}