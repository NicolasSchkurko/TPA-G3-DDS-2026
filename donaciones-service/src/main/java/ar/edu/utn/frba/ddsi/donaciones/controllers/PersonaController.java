package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.PersonaDonanteDTO;
import ar.edu.utn.frba.ddsi.donaciones.mappers.RepresentanteDTOToObject;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Whatsapp;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.TipoJuridico;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.MapeoCSV;
import ar.edu.utn.frba.ddsi.donaciones.services.PersonaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/personas")
public class PersonaController {

  private final PersonaService personaService;

  public PersonaController(PersonaService personaService) {
    this.personaService = personaService;
  }

  // CREATE (C)
  @PostMapping
  public ResponseEntity<?> registrarPersona(@RequestBody PersonaDonanteDTO dto) {
    try {
      Donante dominio = mapearADominio(dto);
      Donante personaCreada = personaService.crearPersona(dominio);
      return new ResponseEntity<>(mapToDto(personaCreada), HttpStatus.CREATED);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  // READ (R) - Obtener todas
  @GetMapping
  public ResponseEntity<List<PersonaDonanteDTO>> obtenerTodas() {
    List<PersonaDonanteDTO> result = personaService.listarTodas().stream()
                                                   .map(this::mapToDto)
                                                   .collect(Collectors.toList());
    return ResponseEntity.ok(result);
  }

  // READ (R) - Búsqueda por ID
  @GetMapping("/{id}")
  public ResponseEntity<PersonaDonanteDTO> obtenerPersonaPorId(@PathVariable UUID id) {
    try {
      return ResponseEntity.ok(mapToDto(personaService.buscarPorId(id)));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  // READ (R) - Búsqueda por nombre mediante QueryParam
  @GetMapping("/buscar")
  public ResponseEntity<PersonaDonanteDTO> buscarPersonaPorNombre(@RequestParam String nombre) {
    Donante resultado = personaService.buscarPorNombre(nombre);
    if (resultado == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(mapToDto(resultado));
  }

  // UPDATE (U)
  @PutMapping("/{id}")
  public ResponseEntity<?> actualizarPersona(@PathVariable UUID id, @RequestBody PersonaDonanteDTO dto) {
    try {
      Donante dominio = mapearADominio(dto);
      Donante actualizada = personaService.actualizarPersona(id, dominio);
      return ResponseEntity.ok(mapToDto(actualizada));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  // DELETE (D)
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarPersona(@PathVariable UUID id) {
    personaService.eliminarPersona(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/importar")
  public ResponseEntity<String> importarPersonasCSV(
      @RequestPart("file") MultipartFile file,
      @RequestParam("mapeos") String mapeosDtoJson) {
    try {
      if (file.isEmpty()) {
        return ResponseEntity.badRequest().body("El archivo enviado está vacío.");
      }
      ObjectMapper objectMapper = new ObjectMapper();
      List<MapeoCSV> mapeosDominio = objectMapper.readValue(
          mapeosDtoJson,
          new TypeReference<List<MapeoCSV>>() {}
      );
      String mensaje = personaService.importarDonantes(file, mapeosDominio);
      return ResponseEntity.status(HttpStatus.ACCEPTED).body(mensaje);
    } catch (RuntimeException | JsonProcessingException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  // --- ENDPOINTS DE MEDIOS DE CONTACTO ---
  @GetMapping("/{id}/medios-contacto")
  public ResponseEntity<List<MediosContactoDTO>> obtenerMediosContacto(@PathVariable UUID id) {
    try {
      return ResponseEntity.ok(mapMediosContactoToDto(personaService.obtenerMediosContacto(id)));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping("/{id}/medios-contacto")
  public ResponseEntity<?> agregarMedioContacto(@PathVariable UUID id, @RequestBody MediosContactoDTO dto) {
    try {
      MedioDeContacto nuevoMedio = pasarMediosDeContactoDTOAObjeto(dto);
      Donante actualizada = personaService.agregarMedioContacto(id, nuevoMedio);
      return new ResponseEntity<>(mapToDto(actualizada), HttpStatus.CREATED);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  private Donante mapearADominio(PersonaDonanteDTO dto) {
    Direccion direccion = mapearDireccionDesdeDTO(dto.getDireccion());
    List<MedioDeContacto> medios = new ArrayList<>();
    MedioDeContacto medioPredeterminado = null;

    for (MediosContactoDTO medioDTO : dto.getMediosDeContacto()) {
      MedioDeContacto nuevoMedio = pasarMediosDeContactoDTOAObjeto(medioDTO);
      medios.add(nuevoMedio);
      if (medioPredeterminado == null) {
        medioPredeterminado = nuevoMedio;
      }
      if (nuevoMedio.toString().equalsIgnoreCase(dto.getMedioPredeterminado().getTipo()) &&
          nuevoMedio.getValor().equalsIgnoreCase(dto.getMedioPredeterminado().getValor())) {
        medioPredeterminado = nuevoMedio;
      }
    }
    if (medioPredeterminado == null) {
      throw new RuntimeException("Error al registrar medio predeterminado");
    }

    if ("HUMANA".equalsIgnoreCase(dto.getTipoPersona())) {
      Genero genero = Genero.valueOf(dto.getGenero().toUpperCase());
      Humana humana = new Humana(dto.getNombre(), dto.getApellido(), dto.getEdad(), dto.getNumeroDeDocumento(), genero);
      PersonaHumana nuevaPersona = new PersonaHumana(humana, direccion, dto.getNombreAMostrar());
      // Genera su nombre de usuario como requiere la peticion
      nuevaPersona.getPersona().setNombreDeUsuario(dto.getNombreDeUsuario());
      nuevaPersona.getMediosDeContacto().agregarMediosDeContacto(medios);
      nuevaPersona.getMediosDeContacto().setMedioDeContactoPredeterminado(medioPredeterminado);
      return nuevaPersona;
    } else if ("JURIDICA".equalsIgnoreCase(dto.getTipoPersona())) {
      TipoJuridico tipo = TipoJuridico.valueOf(dto.getTipoJuridico().toUpperCase());
      Juridica nuevaPersona = new Juridica(
          dto.getRazonSocial(), dto.getRubro(), tipo, dto.getCuit(), new ArrayList<>(), dto.getNombreAMostrar()
      );
      nuevaPersona.setNombreDeUsuario(dto.getNombreDeUsuario());
      nuevaPersona.setDireccion(direccion);
      nuevaPersona.agregarRepresentantes(RepresentanteDTOToObject.convertirEnObjeto(dto.getRepresentantes()));
      nuevaPersona.getMediosDeContacto().agregarMediosDeContacto(medios);
      nuevaPersona.getMediosDeContacto().setMedioDeContactoPredeterminado(medioPredeterminado);
      return nuevaPersona;
    } else {
      throw new IllegalArgumentException("Tipo de persona inválido");
    }
  }

  private PersonaDonanteDTO mapToDto(Donante entidad) {
    if (entidad == null) return null;
    PersonaDonanteDTO responseDTO = new PersonaDonanteDTO();
    responseDTO.setId(entidad.getId());
    try { responseDTO.setNombreAMostrar(entidad.darNombre()); }
    catch (Exception e) { responseDTO.setNombreAMostrar(null); }

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
    } else if (entidad instanceof Juridica pj) {
      responseDTO.setTipoPersona("JURIDICA");
      responseDTO.setRazonSocial(pj.getRazonSocial());
      responseDTO.setCuit(pj.getCuit());
      responseDTO.setRubro(pj.getRubro());
      responseDTO.setTipoJuridico(pj.getTipoJuridico() != null ? pj.getTipoJuridico().name() : null);
      direccion = pj.getDireccion();
    } else {
      responseDTO.setTipoPersona("DESCONOCIDO");
    }

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
                               if (medio instanceof Mail) tipo = "EMAIL";
                               else if (medio instanceof Whatsapp) tipo = "WHATSAPP";
                               else if (medio instanceof Telefono) tipo = "TELEFONO";
                               else tipo = "DESCONOCIDO";
                               return new MediosContactoDTO(tipo, medio.getValor());
                             }).collect(Collectors.toList());
    }
    return new ArrayList<>();
  }

  private Direccion mapearDireccionDesdeDTO(DireccionDTO dto) {
    if (dto == null) return null;
    Pais pais = new Pais(dto.getPais());
    Provincia provincia = new Provincia(dto.getProvincia(), pais);
    Ciudad ciudad = new Ciudad(dto.getCiudad(), provincia);
    return new Direccion(dto.getCalleUno(), dto.getCalleDos(), dto.getAltura(), dto.getPiso(), dto.getDepartamento(), ciudad);
  }

  private MedioDeContacto pasarMediosDeContactoDTOAObjeto(MediosContactoDTO dto) {
    return switch (dto.getTipo().toUpperCase()) {
      case "EMAIL" -> new Mail(dto.getValor().toLowerCase());
      case "TELEFONO" -> new Telefono(dto.getValor());
      case "WHATSAPP" -> new Whatsapp(dto.getValor());
      default -> throw new IllegalArgumentException("Tipo de medio de contacto no soportado.");
    };
  }
}