package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IDDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.ResultadoLotePerfilesDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.PersonaDonanteDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.ReporteImportacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.ResultadoLectura;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.LectorCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.MapeoCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter.PersonaDonanteFilaConverter;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioCiudades;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioDonantes;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioPersonas;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

@Service
public class DonanteService {

  /** Máximo de perfiles por llamada al alta en lote de incentivos (contrato: 500). */
  private static final int TAMANO_LOTE_PERFILES = 500;

  private final RepositorioPersonas repositorioPersonas;
  private final RepositorioCiudades repositorioCiudades;
  private final FabricaEstrategiasNotificacion fabricaEstrategias;
  private final IncentivosClient incentivosClient;
  private final RepositorioDonantes repositorioDonantes;

  /** Reportes en memoria de importaciones CSV en curso/terminadas, consultables por id. */
  private final Map<UUID, ReporteImportacionDTO> reportesImportacion = new ConcurrentHashMap<>();

  /** Pool propio (no el commonPool): una importación grande no compite por los hilos del resto.
   *  Es un bean con cola acotada y shutdown administrado por Spring (ver DonacionesServiceApplication). */
  private final ExecutorService executorImportacion;

  public DonanteService(RepositorioPersonas repositorioPersonas,
                        RepositorioCiudades repositorioCiudades,
                        FabricaEstrategiasNotificacion fabricaEstrategias,
                        IncentivosClient incentivosClient, RepositorioDonantes repositorioDonantes,
                        ExecutorService executorImportacion) {
    this.repositorioPersonas = repositorioPersonas;
    this.repositorioCiudades = repositorioCiudades;
    this.fabricaEstrategias = fabricaEstrategias;
    this.incentivosClient = incentivosClient;
    this.repositorioDonantes = repositorioDonantes;
    this.executorImportacion = executorImportacion;
  }

  public PersonaDonanteDTO crearPersona(PersonaDonanteDTO dto) {
    Ciudad ciudad = resolverCiudad(dto.getDireccion());
    Donante nuevoDonante = dto.toDomain(ciudad);

    if (nuevoDonante.getPersona() != null) {
      incentivosClient.peticionCrearPerfil(perfilDe(nuevoDonante));
    }

    return persistirConNotificacion(nuevoDonante);
  }

  private IDDTO perfilDe(Donante donante) {
    return new IDDTO(donante.getId(), donante.getPersona().getNombreDeUsuario(), "DONANTE");
  }

  /** Notificación + persistencia local del donante, sin el alta de perfil en incentivos
   *  (esa se hace sola al dar de alta o en lote durante la importación CSV). */
  private PersonaDonanteDTO persistirConNotificacion(Donante nuevoDonante) {
    if (nuevoDonante.getPersona() != null) {
      fabricaEstrategias.ejecutar(TipoEventoNotificacion.REGISTRO_PERSONA, nuevoDonante);
      repositorioPersonas.registrarPersona(nuevoDonante.getPersona());
    }
    registrarDonante(nuevoDonante);
    return PersonaDonanteDTO.from(nuevoDonante);
  }

  public List<PersonaDonanteDTO> listarTodas() {
    return repositorioDonantes.obtenerTodos().stream()
                         .map(PersonaDonanteDTO::from)
                         .collect(Collectors.toList());
  }

  public PersonaDonanteDTO buscarPorId(UUID id) {
    Donante donante = repositorioDonantes.buscarPorId(id).orElse(null);
    if (donante == null) throw new IllegalArgumentException("No se encontró persona con ID: " + id);
    return PersonaDonanteDTO.from(donante);
  }

  public PersonaDonanteDTO buscarPorNombre(String nombre) {
    return repositorioDonantes.obtenerTodos().stream()
                         .filter(d -> d.getPersona().getNombreDeUsuario().equalsIgnoreCase(nombre))
                         .findFirst()
                         .map(PersonaDonanteDTO::from).orElse(null);
  }

  public PersonaDonanteDTO actualizarPersona(UUID id, PersonaDonanteDTO dto) {
    Ciudad ciudad = resolverCiudad(dto.getDireccion());
    Donante datosNuevos = dto.toDomain(ciudad);
    Donante existente = repositorioDonantes.buscarPorId(id).orElse(null);
    if (existente == null) throw new IllegalArgumentException("No se encontró persona con ID: " + id);
    if (existente.getPersona() != null && datosNuevos.getPersona() != null) {
      repositorioPersonas.modificarPersona(existente.getPersona().getId(), datosNuevos.getPersona());
    }
    return PersonaDonanteDTO.from(repositorioDonantes.modificarDonante(id, datosNuevos));
  }

  public void eliminarPersona(UUID id) {
    repositorioDonantes.eliminarPorId(id);
    System.out.println("Donante dado de baja (si existía).");
  }

  public UUID importarDonantes(MultipartFile file, List<MapeoCSV> mapeosCsv) {
    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException e) {
      throw new RuntimeException("No se pudo leer el archivo CSV", e);
    }

    UUID importId = UUID.randomUUID();
    reportesImportacion.put(importId, ReporteImportacionDTO.enProgreso(importId));
    try {
      executorImportacion.submit(() -> procesarImportacion(importId, bytes, mapeosCsv));
    } catch (RejectedExecutionException colaLLena) {
      // Si no hay lugar para procesarla, no puede quedar un reporte fantasma "en progreso".
      reportesImportacion.remove(importId);
      throw colaLLena;
    }
    return importId;
  }

  private void procesarImportacion(UUID importId, byte[] bytes, List<MapeoCSV> mapeosCsv) {
    List<String> errores = new ArrayList<>();
    int totalFilas = 0;
    int exitosos = 0;

    try {
      try (InputStream is = new ByteArrayInputStream(bytes)) {
        LectorCSV<Donante> lector = new LectorCSV<>(',', new PersonaDonanteFilaConverter(mapeosCsv));
        ResultadoLectura<Donante> resultado = lector.importar(is);
        totalFilas = resultado.getTotalFilas();
        errores.addAll(resultado.getErrores());

        List<IDDTO> perfilesPendientes = new ArrayList<>();
        for (Donante d : resultado.getElementos()) {
          try {
            persistirConNotificacion(d);
            perfilesPendientes.add(perfilDe(d));
            exitosos++;
          } catch (Exception e) {
            errores.add("Donante '" + d.getPersona().getNombreDeUsuario() + "': " + e.getMessage());
          }
        }

        crearPerfilesEnLote(perfilesPendientes, errores);
      } catch (Exception e) {
        errores.add("Error inesperado al procesar el archivo: " + e.getMessage());
      }
    } finally {
      // El reporte se cierra siempre: ni un Error que mate al worker puede dejarlo
      // "EN_PROGRESO" para siempre.
      reportesImportacion.put(importId,
          ReporteImportacionDTO.completado(importId, totalFilas, exitosos, errores));
    }
  }

  /** Alta de perfiles en bloques de 500: misma cantidad de datos, con dos órdenes de
   *  magnitud menos de ida y vuelta contra incentivos que un POST por donante. */
  private void crearPerfilesEnLote(List<IDDTO> perfiles, List<String> errores) {
    for (int inicio = 0; inicio < perfiles.size(); inicio += TAMANO_LOTE_PERFILES) {
      List<IDDTO> lote = perfiles.subList(inicio,
          Math.min(inicio + TAMANO_LOTE_PERFILES, perfiles.size()));
      try {
        ResultadoLotePerfilesDTO respuesta = incentivosClient.peticionCrearPerfilesEnLote(lote);
        if (respuesta != null && respuesta.getErrores() != null) {
          errores.addAll(respuesta.getErrores());
        }
      } catch (RuntimeException e) {
        errores.add("No se pudieron crear " + lote.size() + " perfiles en incentivos: " + e.getMessage());
      }
    }
  }

  public ReporteImportacionDTO obtenerReporteImportacion(UUID importId) {
    ReporteImportacionDTO reporte = reportesImportacion.get(importId);
    if (reporte == null) {
      throw new EntityNotFoundException("No se encontró una importación con ID: " + importId);
    }
    return reporte;
  }

  public List<MediosContactoDTO> obtenerMediosContacto(UUID id) {
    Donante donante = repositorioDonantes.buscarPorId(id).orElse(null);
    if (donante == null) throw new IllegalArgumentException("No se encontró persona con ID: " + id);
    if (donante.getPersona().getMediosDeContacto() != null && donante.getPersona().getMediosDeContacto().getListaMediosDeContacto() != null) {
      return donante.getPersona().getMediosDeContacto().getListaMediosDeContacto().stream().map(MediosContactoDTO::from).collect(Collectors.toList());
    }
    return List.of();
  }

  public PersonaDonanteDTO agregarMedioContacto(UUID id, MediosContactoDTO dto) {
    Donante donante = repositorioDonantes.buscarPorId(id).orElse(null);
    if (donante == null) throw new IllegalArgumentException("No se encontró persona con ID: " + id);
    repositorioPersonas.agregarMedioDeContactoAPersona(donante.getPersona().getId(), dto.toDomain());
    return PersonaDonanteDTO.from(donante);
  }

  public PersonaDonanteDTO eliminarMedioContacto(UUID id, MediosContactoDTO dto) {
    Donante donante = repositorioDonantes.buscarPorId(id).orElse(null);
    if (donante == null) throw new IllegalArgumentException("No se encontró persona con ID: " + id);
    repositorioPersonas.eliminarMedioDeContactoAPersona(donante.getPersona().getId(), dto.toDomain());
    return PersonaDonanteDTO.from(donante);
  }

  public void revisarActividades(){
    for (Donante p : repositorioDonantes.obtenerTodos()) {
      if (p.getFormularios() != null && !p.getFormularios().isEmpty() && p.getFormularios().getLast().getFechaRealizacion().plusDays(20).isBefore(LocalDate.now())) {
        fabricaEstrategias.ejecutar(TipoEventoNotificacion.INACTIVIDAD_PERSONA_DONANTE, p);
      }
    }
  }

  // Resuelve (o crea) la Ciudad/Provincia/Pais del catálogo geográfico compartido ANTES de
  // construir el domain object, para evitar que Direccion apunte a una Ciudad nunca persistida
  // (eso rompía merge() con EntityNotFoundException, ver RepositorioCiudades / EntidadBeneficiariaService).
  private Ciudad resolverCiudad(DireccionDTO direccionDTO) {
    if (direccionDTO == null) return null;
    return repositorioCiudades.obtenerOCrearCiudad(direccionDTO.getPais(), direccionDTO.getProvincia(), direccionDTO.getCiudad());
  }

  private void registrarDonante(Donante nuevoDonante) {
    try {
      repositorioDonantes.guardar(nuevoDonante);
      System.out.println("Donante registrado con éxito con ID: " + nuevoDonante.getId());
    } catch (IllegalArgumentException e) {
      System.err.println("Error al registrar donante: " + e.getMessage());
    }
  }
}