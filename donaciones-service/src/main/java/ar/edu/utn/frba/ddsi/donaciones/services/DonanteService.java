package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.clients.NotificacionesClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IDDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.PersonaDonanteDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.RepresentanteDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Whatsapp;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Representante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.TipoJuridico;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.LectorCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.MapeoCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter.PersonaDonanteFilaConverter;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorDonantes;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorPersonas;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class DonanteService {

  private final GestorDonantes gestorDonantes;
  private final GestorPersonas gestorPersonas;
  private final NotificacionesClient notificacionClient;
  private final FabricaEstrategiasNotificacion fabricaEstrategias;
  private final IncentivosClient incentivosClient;

  public DonanteService(GestorDonantes gestorDonantes,
                        GestorPersonas gestorPersonas,
                        NotificacionesClient notificacionClient,
                        FabricaEstrategiasNotificacion fabricaEstrategias,
                        IncentivosClient incentivosClient) {
    this.gestorDonantes = gestorDonantes;
    this.gestorPersonas = gestorPersonas;
    this.notificacionClient = notificacionClient;
    this.fabricaEstrategias = fabricaEstrategias;
    this.incentivosClient = incentivosClient;
  }

  public PersonaDonanteDTO crearPersona(PersonaDonanteDTO dto) {
    Donante nuevoDonante = mapearADominio(dto);
    return mapToDto(procesarNuevoDonante(nuevoDonante));
  }

  public List<PersonaDonanteDTO> listarTodas() {
    return gestorDonantes.listarTodosLosDonantes().stream()
                         .map(this::mapToDto)
                         .collect(Collectors.toList());
  }

  public PersonaDonanteDTO buscarPorId(UUID id) {
    Donante donante = gestorDonantes.obtenerDonante(id);
    if (donante == null) {
      throw new IllegalArgumentException("No se encontró la persona con ID: " + id);
    }
    return mapToDto(donante);
  }

  public PersonaDonanteDTO buscarPorNombre(String nombreBuscado) {
    Donante donante = gestorDonantes.listarTodosLosDonantes().stream()
                                    .filter(d -> d.getPersona().getNombreDeUsuario().equalsIgnoreCase(nombreBuscado))
                                    .findFirst()
                                    .orElse(null);
    return mapToDto(donante);
  }

  public PersonaDonanteDTO actualizarPersona(UUID id, PersonaDonanteDTO dto) {
    Donante datosNuevos = mapearADominio(dto);
    Donante existente = gestorDonantes.obtenerDonante(id);
    if (existente == null) {
      throw new IllegalArgumentException("No se encontró la persona con ID: " + id);
    }

    if (existente.getPersona() != null && datosNuevos.getPersona() != null) {
      gestorPersonas.modificarPersona(existente.getPersona().getId(), datosNuevos.getPersona());
    }
    Donante actualizado = gestorDonantes.modificarDonante(id, datosNuevos);
    return mapToDto(actualizado);
  }

  public void eliminarPersona(UUID id) {
    gestorDonantes.darDeBajaDonante(id);
  }

  public String importarDonantes(MultipartFile file, List<MapeoCSV> mapeosCsv) {
    try {
      byte[] fileBytes = file.getBytes();
      CompletableFuture.runAsync(() -> {
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
          PersonaDonanteFilaConverter conversor = new PersonaDonanteFilaConverter(mapeosCsv);
          LectorCSV<Donante> lectorTemporal = new LectorCSV<>(',', conversor);
          List<Donante> donantesImportados = lectorTemporal.importar(inputStream);

          int guardados = 0;
          for (Donante donante : donantesImportados) {
            try {
              procesarNuevoDonante(donante);
              guardados++;
            } catch (Exception e) {
              System.err.println("Fallo al persistir un donante del CSV: " + e.getMessage());
            }
          }
          System.out.println("Importación CSV finalizada. Se guardaron " + guardados + " donantes.");
        } catch (IOException e) {
          System.err.println("Error de IO al leer el stream del CSV: " + e.getMessage());
        }
      });
      return "El archivo fue recibido con éxito. La importación se está ejecutando en segundo plano.";
    } catch (IOException e) {
      throw new RuntimeException("Error al acceder al archivo CSV", e);
    }
  }

  public List<MediosContactoDTO> obtenerMediosContacto(UUID id) {
    Donante donante = gestorDonantes.obtenerDonante(id);
    if (donante == null) {
      throw new IllegalArgumentException("No se encontró la persona con ID: " + id);
    }
    return mapMediosContactoToDto(donante.getPersona().getMediosDeContacto());
  }

  public PersonaDonanteDTO agregarMedioContacto(UUID id, MediosContactoDTO dto) {
    Donante donante = gestorDonantes.obtenerDonante(id);
    if (donante == null) {
      throw new IllegalArgumentException("No se encontró la persona con ID: " + id);
    }
    MedioDeContacto nuevoMedio = pasarMediosDeContactoDTOAObjeto(dto);
    gestorPersonas.agregarMedioDeContactoAPersona(donante.getPersona().getId(), nuevoMedio);
    return mapToDto(donante);
  }

  private Donante procesarNuevoDonante(Donante nuevoDonante) {
    if (nuevoDonante.getPersona() != null) {
      String nombreUsuario = nuevoDonante.getPersona().getNombreDeUsuario();
      IDDTO peticion = new IDDTO(nuevoDonante.getId(), nombreUsuario);
      incentivosClient.peticionCrearPerfil(peticion);

      if (nuevoDonante.getPersona().getMediosDeContacto() != null &&
          nuevoDonante.getPersona().getMediosDeContacto().getMedioDeContactoPredeterminado() != null) {

        MedioDeContacto predeterminado = nuevoDonante.getPersona().getMediosDeContacto().getMedioDeContactoPredeterminado();
        NotificacionDTO notificacionCreacionUsuario = new NotificacionDTO(
            predeterminado.getTipo().toLowerCase(),
            predeterminado.getValor(),
            "Gracias por registrarse en DonaTrack",
            "Nuevo Registro en DonaTrack"
        );
        notificacionClient.enviarNotificacion(notificacionCreacionUsuario);
      }
      gestorPersonas.registrarPersona(nuevoDonante.getPersona());
    }
    gestorDonantes.registrarDonante(nuevoDonante);
    return nuevoDonante;
  }

  public void revisarActividades(){
    List<Donante> personas = gestorDonantes.listarTodosLosDonantes();
    for (Donante persona : personas) {
      if (persona.getFormularios() != null && !persona.getFormularios().isEmpty()) {
        if (persona.getFormularios().getLast().getFechaRealizacion().plusDays(20).isBefore(LocalDate.now())) {
          fabricaEstrategias.obtenerEstrategia(TipoEventoNotificacion.INACTIVIDAD_PERSONA_DONANTE).ejecutar(persona);
        }
      }
    }
  }

  // --- MAPPERS INTERNOS ---

  private Donante mapearADominio(PersonaDonanteDTO dto) {
    Direccion direccion = mapearDireccionDesdeDTO(dto.getDireccion());
    List<MedioDeContacto> medios = new ArrayList<>();
    MedioDeContacto medioPredeterminado = null;

    if (dto.getMediosDeContacto() != null) {
      for (MediosContactoDTO medioDTO : dto.getMediosDeContacto()) {
        MedioDeContacto nuevoMedio = pasarMediosDeContactoDTOAObjeto(medioDTO);
        if (nuevoMedio == null) continue;
        medios.add(nuevoMedio);

        if (medioPredeterminado == null) {
          medioPredeterminado = nuevoMedio;
        }

        if (dto.getMedioPredeterminado() != null &&
            nuevoMedio.getTipo().equalsIgnoreCase(dto.getMedioPredeterminado().getTipo()) &&
            nuevoMedio.getValor().equalsIgnoreCase(dto.getMedioPredeterminado().getValor())) {
          medioPredeterminado = nuevoMedio;
        }
      }
    }

    if (medioPredeterminado == null && !medios.isEmpty()) {
      medioPredeterminado = medios.get(0);
    } else if (medioPredeterminado == null) {
      throw new IllegalArgumentException("Debe proveer al menos un medio de contacto válido.");
    }

    String tipoPer = dto.getTipoPersona() != null ? dto.getTipoPersona().toUpperCase() : "HUMANA";

    if ("HUMANA".equals(tipoPer)) {
      Genero genero = dto.getGenero() != null ? Genero.valueOf(dto.getGenero().toUpperCase()) : Genero.OTRO;
      Humana humana = new Humana(dto.getNombre(), dto.getApellido(), dto.getEdad(), dto.getNumeroDeDocumento(), genero, dto.getNombreAMostrar());
      Donante nuevaPersona = new Donante(direccion, humana);
      nuevaPersona.getPersona().getMediosDeContacto().agregarMediosDeContacto(medios);
      nuevaPersona.getPersona().getMediosDeContacto().setMedioDeContactoPredeterminado(medioPredeterminado);
      return nuevaPersona;
    } else if ("JURIDICA".equals(tipoPer)) {
      TipoJuridico tipo = dto.getTipoJuridico() != null ? TipoJuridico.valueOf(dto.getTipoJuridico().toUpperCase()) : TipoJuridico.ONG;
      Juridica nuevaPersona = new Juridica(
          dto.getRazonSocial(), dto.getRubro(), tipo, dto.getCuit(), new ArrayList<>(), dto.getNombreAMostrar()
      );
      nuevaPersona.getMediosDeContacto().agregarMediosDeContacto(medios);
      nuevaPersona.getMediosDeContacto().setMedioDeContactoPredeterminado(medioPredeterminado);
      Donante nuevoDonante = new Donante(direccion, nuevaPersona);

      if (dto.getRepresentantes() != null && !dto.getRepresentantes().isEmpty()) {
        List<Representante> representantes = dto.getRepresentantes().stream().map(rep -> {
          Humana h = new Humana(rep.getNombre(), rep.getApellido(), rep.getEdad(), rep.getNumeroDeDocumento(),
                                rep.getGenero() != null ? Genero.valueOf(rep.getGenero().toUpperCase()) : Genero.OTRO,
                                rep.getNombre());
          return new Representante(h, rep.isActivo());
        }).collect(Collectors.toList());
        ((Juridica) nuevoDonante.getPersona()).agregarRepresentantes(representantes);
      }
      return nuevoDonante;
    } else {
      throw new IllegalArgumentException("Tipo de persona inválido: " + dto.getTipoPersona());
    }
  }

  private PersonaDonanteDTO mapToDto(Donante entidad) {
    if (entidad == null) return null;
    PersonaDonanteDTO responseDTO = new PersonaDonanteDTO();
    responseDTO.setId(entidad.getId());

    if (entidad.getPersona() != null) {
      responseDTO.setNombreAMostrar(entidad.getPersona().getNombreDeUsuario());
      responseDTO.setMediosDeContacto(mapMediosContactoToDto(entidad.getPersona().getMediosDeContacto()));

      if (entidad.getPersona().getMediosDeContacto() != null && entidad.getPersona().getMediosDeContacto().getMedioDeContactoPredeterminado() != null) {
        MedioDeContacto pred = entidad.getPersona().getMediosDeContacto().getMedioDeContactoPredeterminado();
        responseDTO.setMedioPredeterminado(new MediosContactoDTO(pred.getTipo(), pred.getValor()));
      }

      if (entidad.getPersona() instanceof Humana ph) {
        responseDTO.setTipoPersona("HUMANA");
        responseDTO.setNombre(ph.getNombre());
        responseDTO.setApellido(ph.getApellido());
        responseDTO.setEdad(ph.getEdad());
        responseDTO.setNumeroDeDocumento(ph.getNumeroDeDocumento());
        responseDTO.setGenero(ph.getGenero() != null ? ph.getGenero().name() : null);
      } else if (entidad.getPersona() instanceof Juridica pj) {
        responseDTO.setTipoPersona("JURIDICA");
        responseDTO.setRazonSocial(pj.getRazonSocial());
        responseDTO.setCuit(pj.getCuit());
        responseDTO.setRubro(pj.getRubro());
        responseDTO.setTipoJuridico(pj.getTipoJuridico() != null ? pj.getTipoJuridico().name() : null);

        if (pj.getRepresentantes() != null) {
          List<RepresentanteDTO> repsDto = pj.getRepresentantes().stream().map(r -> {
            RepresentanteDTO rdto = new RepresentanteDTO();
            rdto.setActivo(r.isActivo());
            // AQUÍ ESTÁ EL ARREGLO: usamos getHumana()
            if (r.getHumana() != null) {
              rdto.setNombre(r.getHumana().getNombre());
              rdto.setApellido(r.getHumana().getApellido());
              rdto.setEdad(r.getHumana().getEdad());
              rdto.setNumeroDeDocumento(r.getHumana().getNumeroDeDocumento());
              rdto.setGenero(r.getHumana().getGenero() != null ? r.getHumana().getGenero().name() : null);
            }
            return rdto;
          }).collect(Collectors.toList());
          responseDTO.setRepresentantes(repsDto);
        }
      } else {
        responseDTO.setTipoPersona("DESCONOCIDO");
      }
    }

    Direccion direccion = entidad.getDireccion();
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

    return responseDTO;
  }

  private List<MediosContactoDTO> mapMediosContactoToDto(MediosDeContacto mediosDeContacto) {
    if (mediosDeContacto != null && mediosDeContacto.getListaMediosDeContacto() != null) {
      return mediosDeContacto.getListaMediosDeContacto().stream()
                             .filter(java.util.Objects::nonNull)
                             .map(medio -> new MediosContactoDTO(medio.getTipo(), medio.getValor()))
                             .collect(Collectors.toList());
    }
    return new ArrayList<>();
  }

  private Direccion mapearDireccionDesdeDTO(DireccionDTO dto) {
    if (dto == null) return null;
    Pais pais = new Pais(dto.getPais() != null ? dto.getPais() : "Argentina");
    Provincia provincia = new Provincia(dto.getProvincia() != null ? dto.getProvincia() : "Buenos Aires", pais);
    Ciudad ciudad = new Ciudad(dto.getCiudad() != null ? dto.getCiudad() : "CABA", provincia);
    return new Direccion(dto.getCalleUno(), dto.getCalleDos(), dto.getAltura(), dto.getPiso(), dto.getDepartamento(), ciudad);
  }

  private MedioDeContacto pasarMediosDeContactoDTOAObjeto(MediosContactoDTO dto) {
    if (dto == null || dto.getTipo() == null || dto.getValor() == null) return null;
    return switch (dto.getTipo().toUpperCase()) {
      case "EMAIL" -> new Mail(dto.getValor().toLowerCase());
      case "TELEFONO" -> new Telefono(dto.getValor());
      case "WHATSAPP" -> new Whatsapp(dto.getValor());
      default -> throw new IllegalArgumentException("Tipo de medio de contacto no soportado: " + dto.getTipo());
    };
  }
}