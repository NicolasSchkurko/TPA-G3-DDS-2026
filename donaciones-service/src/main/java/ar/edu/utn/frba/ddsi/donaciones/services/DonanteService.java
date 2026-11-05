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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioFormularios;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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

  private static final Logger log = LoggerFactory.getLogger(DonanteService.class);

  private final RepositorioPersonas repositorioPersonas;
  private final RepositorioCiudades repositorioCiudades;
  private final FabricaEstrategiasNotificacion fabricaEstrategias;
  private final IncentivosClient incentivosClient;
  private final RepositorioDonantes repositorioDonantes;
  private final RepositorioDonaciones repositorioDonaciones;
  private final RepositorioFormularios repositorioFormularios;

  private static final int MAX_REPORTES = 100;

  /** Reportes en memoria de importaciones CSV acotados a las últimas 100 con política LRU (Punto 73). */
  private final Map<UUID, ReporteImportacionDTO> reportesImportacion =
      Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, ReporteImportacionDTO> eldest) {
          return size() > MAX_REPORTES;
        }
      });

  /** Pool propio (no el commonPool): una importación grande no compite por los hilos del resto.
   *  Es un bean con cola acotada y shutdown administrado por Spring (ver DonacionesServiceApplication). */
  private final ExecutorService executorImportacion;

  public DonanteService(RepositorioPersonas repositorioPersonas,
                        RepositorioCiudades repositorioCiudades,
                        FabricaEstrategiasNotificacion fabricaEstrategias,
                        IncentivosClient incentivosClient,
                        RepositorioDonantes repositorioDonantes,
                        ExecutorService executorImportacion) {
    this(repositorioPersonas, repositorioCiudades, fabricaEstrategias, incentivosClient,
         repositorioDonantes, null, null, executorImportacion);
  }

  @org.springframework.beans.factory.annotation.Autowired
  public DonanteService(RepositorioPersonas repositorioPersonas,
                        RepositorioCiudades repositorioCiudades,
                        FabricaEstrategiasNotificacion fabricaEstrategias,
                        IncentivosClient incentivosClient,
                        RepositorioDonantes repositorioDonantes,
                        RepositorioDonaciones repositorioDonaciones,
                        RepositorioFormularios repositorioFormularios,
                        ExecutorService executorImportacion) {
    this.repositorioPersonas = repositorioPersonas;
    this.repositorioCiudades = repositorioCiudades;
    this.fabricaEstrategias = fabricaEstrategias;
    this.incentivosClient = incentivosClient;
    this.repositorioDonantes = repositorioDonantes;
    this.repositorioDonaciones = repositorioDonaciones;
    this.repositorioFormularios = repositorioFormularios;
    this.executorImportacion = executorImportacion;
  }

  @Transactional(rollbackFor = Exception.class)
  public PersonaDonanteDTO crearPersona(PersonaDonanteDTO dto) {
    Ciudad ciudad = resolverCiudad(dto.getDireccion());
    Donante nuevoDonante = dto.toDomain(ciudad);

    // Punto 52: Persistir localmente primero en transacción
    if (nuevoDonante.getPersona() != null) {
      repositorioPersonas.registrarPersona(nuevoDonante.getPersona());
    }
    repositorioDonantes.guardar(nuevoDonante);

    // Notificaciones y creación de perfil en incentivos después del commit local
    Runnable sincronizacionExterna = () -> {
      if (nuevoDonante.getPersona() != null) {
        if (incentivosClient != null) {
          try {
            incentivosClient.peticionCrearPerfil(perfilDe(nuevoDonante));
          } catch (Exception e) {
            log.warn("No se pudo crear perfil en incentivos para donante {}: {}", nuevoDonante.getId(), e.getMessage());
          }
        }
        try {
          fabricaEstrategias.ejecutar(TipoEventoNotificacion.REGISTRO_PERSONA, nuevoDonante);
        } catch (Exception e) {
          log.warn("No se pudo enviar notificación de registro para donante {}: {}", nuevoDonante.getId(), e.getMessage());
        }
      }
    };

    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          sincronizacionExterna.run();
        }
      });
    } else {
      sincronizacionExterna.run();
    }

    return PersonaDonanteDTO.from(nuevoDonante);
  }

  private IDDTO perfilDe(Donante donante) {
    return new IDDTO(donante.getId(), donante.getPersona().getNombreDeUsuario(), "DONANTE");
  }

  /** Notificación + persistencia local del donante para importación CSV (Punto 52: persistencia primero). */
  private PersonaDonanteDTO persistirConNotificacion(Donante nuevoDonante) {
    if (nuevoDonante.getPersona() != null) {
      repositorioPersonas.registrarPersona(nuevoDonante.getPersona());
      try {
        fabricaEstrategias.ejecutar(TipoEventoNotificacion.REGISTRO_PERSONA, nuevoDonante);
      } catch (Exception e) {
        log.warn("No se pudo enviar notificación de registro para donante {}: {}", nuevoDonante.getId(), e.getMessage());
      }
    }
    repositorioDonantes.guardar(nuevoDonante);
    return PersonaDonanteDTO.from(nuevoDonante);
  }

  public List<PersonaDonanteDTO> listarTodas() {
    return repositorioDonantes.obtenerTodos().stream()
                         .filter(Donante::isActivo)
                         .map(PersonaDonanteDTO::from)
                         .collect(Collectors.toList());
  }

  public Page<PersonaDonanteDTO> listarTodas(Pageable pageable) {
    return repositorioDonantes.obtenerTodosActivos(pageable).map(PersonaDonanteDTO::from);
  }

  public PersonaDonanteDTO buscarPorId(UUID id) {
    Donante donante = repositorioDonantes.buscarPorId(id)
        .orElseThrow(() -> new EntityNotFoundException("No se encontró persona con ID: " + id));
    return PersonaDonanteDTO.from(donante);
  }

  public PersonaDonanteDTO buscarPorNombre(String nombre) {
    if (nombre == null || nombre.isBlank()) {
      return null;
    }
    return repositorioDonantes.buscarPorNombre(nombre)
        .map(PersonaDonanteDTO::from)
        .orElse(null);
  }

  @Transactional(rollbackFor = Exception.class)
  public PersonaDonanteDTO actualizarPersona(UUID id, PersonaDonanteDTO dto) {
    Ciudad ciudad = resolverCiudad(dto.getDireccion());
    Donante datosNuevos = dto.toDomain(ciudad);
    Donante existente = repositorioDonantes.buscarPorId(id)
        .orElseThrow(() -> new EntityNotFoundException("No se encontró persona con ID: " + id));
    if (existente.getPersona() != null && datosNuevos.getPersona() != null) {
      repositorioPersonas.modificarPersona(existente.getPersona().getId(), datosNuevos.getPersona());
    }
    Donante actualizado = repositorioDonantes.modificarDonante(id, datosNuevos);
    if (actualizado != null && actualizado.getPersona() != null && incentivosClient != null) {
      try {
        incentivosClient.actualizarPerfil(id, perfilDe(actualizado));
      } catch (Exception e) {
        log.warn("No se pudo propagar la actualización del donante {} a incentivos: {}", id, e.getMessage());
      }
    }
    return PersonaDonanteDTO.from(actualizado);
  }

  /**
   * Da de baja a un donante del sistema mediante soft delete y anonimización (Punto 69).
   *
   * <p>Punto 69: Aplica borrado lógico (activo = false, fechaBaja = now()) y anonimización de datos
   * personales sensibles (persona y dirección), preservando intacta la clave foránea del donante
   * en donaciones y formularios para evitar NullPointerExceptions en trazabilidad logística.
   *
   * <p>Punto 35: Notifica la baja a incentivos-service SOLO una vez confirmado el soft delete
   * local en la base de datos (después del commit o flush), capturando cualquier excepción para que
   * caídas o demoras de incentivos no aborten la baja local ni provoquen inconsistencias.
   */
  @Transactional(rollbackFor = Exception.class)
  public void eliminarPersona(UUID id) {
    Donante donante = repositorioDonantes.buscarPorId(id).orElse(null);
    if (donante == null) {
      return;
    }

    donante.darDeBaja();
    repositorioDonantes.actualizar(id, donante);
    repositorioDonantes.flush();

    Runnable notificarIncentivos = () -> {
      try {
        incentivosClient.eliminarPerfil(id);
      } catch (Exception e) {
        log.warn("No se pudo notificar la baja a incentivos-service para el donante {}: {}", id, e.getMessage());
      }
    };

    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          notificarIncentivos.run();
        }
      });
    } else {
      notificarIncentivos.run();
    }

    System.out.println("Donante dado de baja con éxito.");
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

  @Transactional(rollbackFor = Exception.class)
  public PersonaDonanteDTO agregarMedioContacto(UUID id, MediosContactoDTO dto) {
    Donante donante = repositorioDonantes.buscarPorId(id).orElse(null);
    if (donante == null) throw new IllegalArgumentException("No se encontró persona con ID: " + id);
    repositorioPersonas.agregarMedioDeContactoAPersona(donante.getPersona().getId(), dto.toDomain());
    return PersonaDonanteDTO.from(donante);
  }

  @Transactional(rollbackFor = Exception.class)
  public PersonaDonanteDTO eliminarMedioContacto(UUID id, MediosContactoDTO dto) {
    Donante donante = repositorioDonantes.buscarPorId(id).orElse(null);
    if (donante == null) throw new IllegalArgumentException("No se encontró persona con ID: " + id);
    repositorioPersonas.eliminarMedioDeContactoAPersona(donante.getPersona().getId(), dto.toDomain());
    return PersonaDonanteDTO.from(donante);
  }

  @Transactional(rollbackFor = Exception.class)
  public void revisarActividades(){
    for (Donante p : repositorioDonantes.obtenerTodos()) {
      if (p.isActivo() && p.getFormularios() != null && !p.getFormularios().isEmpty() && p.getFormularios().getLast().getFechaRealizacion().plusDays(20).isBefore(LocalDate.now())) {
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
