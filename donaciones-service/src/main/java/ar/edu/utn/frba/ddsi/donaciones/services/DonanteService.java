package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IDDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.ResultadoLotePerfilesDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.direccion.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personadonante.PersonaDonanteDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personadonante.ReporteImportacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.ResultadoLectura;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.LectorCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.MapeoCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter.PersonaDonanteFilaConverter;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorCatalogo;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorGeoref;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorIncentivos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeDonantes;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDePersonas;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeFormularios;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class DonanteService {

  /** Máximo de perfiles por llamada al alta en lote de incentivos (contrato: 500). */
  private static final int TAMANO_LOTE_PERFILES = 500;

  /** Donantes inactivos procesados por corrida del scheduler (punto 80). */
  private static final int TAMANO_LOTE_INACTIVIDAD = 500;

  private static final Logger log = LoggerFactory.getLogger(DonanteService.class);

  private final RepositorioDePersonas repositorioPersonas;
  private final GestorCatalogo gestorCatalogo;
  private final FabricaEstrategiasNotificacion fabricaEstrategias;
  private final IncentivosClient incentivosClient;
  private final RepositorioDeDonantes repositorioDonantes;
  private final RepositorioDeDonaciones repositorioDonaciones;
  private final RepositorioDeFormularios repositorioFormularios;
  private final GestorGeoref gestorGeoref;

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
  private final GestorIncentivos gestorIncentivos;

  public DonanteService(RepositorioDePersonas repositorioPersonas,
                        GestorCatalogo gestorCatalogo,
                        FabricaEstrategiasNotificacion fabricaEstrategias,
                        IncentivosClient incentivosClient,
                        RepositorioDeDonantes repositorioDonantes,
                        ExecutorService executorImportacion) {
    this(repositorioPersonas, gestorCatalogo, fabricaEstrategias, incentivosClient,
         repositorioDonantes, null, null, new GestorGeoref(null, false), executorImportacion);
  }

  /** Constructor de conveniencia para tests que no ejercitan geocoding. */
  public DonanteService(RepositorioDePersonas repositorioPersonas,
                        GestorCatalogo gestorCatalogo,
                        FabricaEstrategiasNotificacion fabricaEstrategias,
                        IncentivosClient incentivosClient,
                        RepositorioDeDonantes repositorioDonantes,
                        RepositorioDeDonaciones repositorioDonaciones,
                        RepositorioDeFormularios repositorioFormularios,
                        ExecutorService executorImportacion) {
    this(repositorioPersonas, gestorCatalogo, fabricaEstrategias, incentivosClient,
         repositorioDonantes, repositorioDonaciones, repositorioFormularios,
         new GestorGeoref(null, false), executorImportacion);
  }

  public DonanteService(RepositorioDePersonas repositorioPersonas,
                        GestorCatalogo gestorCatalogo,
                        FabricaEstrategiasNotificacion fabricaEstrategias,
                        IncentivosClient incentivosClient,
                        RepositorioDeDonantes repositorioDonantes,
                        RepositorioDeDonaciones repositorioDonaciones,
                        RepositorioDeFormularios repositorioFormularios,
                        GestorGeoref gestorGeoref,
                        ExecutorService executorImportacion) {
    this(repositorioPersonas, gestorCatalogo, fabricaEstrategias, incentivosClient,
         repositorioDonantes, repositorioDonaciones, repositorioFormularios, gestorGeoref,
         executorImportacion, null);
  }

  @org.springframework.beans.factory.annotation.Autowired
  public DonanteService(RepositorioDePersonas repositorioPersonas,
                        GestorCatalogo gestorCatalogo,
                        FabricaEstrategiasNotificacion fabricaEstrategias,
                        IncentivosClient incentivosClient,
                        RepositorioDeDonantes repositorioDonantes,
                        RepositorioDeDonaciones repositorioDonaciones,
                        RepositorioDeFormularios repositorioFormularios,
                        GestorGeoref gestorGeoref,
                        ExecutorService executorImportacion,
                        GestorIncentivos gestorIncentivos) {
    this.repositorioPersonas = repositorioPersonas;
    this.gestorCatalogo = gestorCatalogo;
    this.fabricaEstrategias = fabricaEstrategias;
    this.incentivosClient = incentivosClient;
    this.repositorioDonantes = repositorioDonantes;
    this.repositorioDonaciones = repositorioDonaciones;
    this.repositorioFormularios = repositorioFormularios;
    this.gestorGeoref = gestorGeoref;
    this.executorImportacion = executorImportacion;
    this.gestorIncentivos = gestorIncentivos;
  }

  @Transactional(rollbackFor = Exception.class)
  public PersonaDonanteDTO crearPersona(PersonaDonanteDTO dto) {
    Ciudad ciudad = resolverCiudad(dto.getDireccion());
    Donante nuevoDonante = dto.toDomain(ciudad);
    gestorGeoref.geocodificarAlta(nuevoDonante.getDireccion());

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

  private void ejecutarDespuesDelCommit(Runnable accion) {
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          accion.run();
        }
      });
    } else {
      accion.run();
    }
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

  @Transactional(readOnly = true)
  public List<PersonaDonanteDTO> listarTodas() {
    return repositorioDonantes.obtenerTodos().stream()
                         .filter(Donante::isActivo)
                         .map(PersonaDonanteDTO::from)
                         .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public Page<PersonaDonanteDTO> listarTodas(Pageable pageable) {
    return repositorioDonantes.obtenerTodosActivos(pageable).map(PersonaDonanteDTO::from);
  }

  @Transactional(readOnly = true)
  public PersonaDonanteDTO buscarPorId(UUID id) {
    Donante donante = repositorioDonantes.buscarPorId(id)
        .orElseThrow(() -> new EntityNotFoundException("No se encontró persona con ID: " + id));
    return PersonaDonanteDTO.from(donante);
  }

  @Transactional(readOnly = true)
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
    Donante existente = repositorioDonantes.buscarPorId(id)
        .orElseThrow(() -> new EntityNotFoundException("No se encontró persona con ID: " + id));

    Ciudad ciudad = resolverCiudad(dto.getDireccion());
    Donante datosNuevos = dto.toDomain(ciudad);
    gestorGeoref.geocodificarActualizacion(datosNuevos.getDireccion(), existente.getDireccion());

    if (existente.getPersona() != null && datosNuevos.getPersona() != null) {
      repositorioPersonas.modificarPersona(existente.getPersona().getId(), datosNuevos.getPersona());
    }
    Donante actualizado = repositorioDonantes.modificarDonante(id, datosNuevos);
    if (actualizado != null && actualizado.getPersona() != null) {
      if (gestorIncentivos != null) {
        UUID outboxId = gestorIncentivos.encolarActualizacionPerfil(id, perfilDe(actualizado));
        ejecutarDespuesDelCommit(() -> gestorIncentivos.intentarAhora(outboxId));
      } else if (incentivosClient != null) {
        try {
          incentivosClient.actualizarPerfil(id, perfilDe(actualizado));
        } catch (Exception e) {
          log.warn("No se pudo propagar la actualización del donante {} a incentivos: {}", id, e.getMessage());
        }
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
    Path archivoTemporal;
    try {
      archivoTemporal = Files.createTempFile("importacion-donantes-", ".csv");
    } catch (IOException e) {
      throw new RuntimeException("No se pudo preparar el archivo CSV", e);
    }
    try {
      file.transferTo(archivoTemporal);
    } catch (IOException | IllegalStateException e) {
      borrarArchivoTemporal(archivoTemporal);
      throw new RuntimeException("No se pudo leer el archivo CSV", e);
    }

    UUID importId = UUID.randomUUID();
    reportesImportacion.put(importId, ReporteImportacionDTO.enProgreso(importId));
    try {
      executorImportacion.submit(() -> {
        try {
          procesarImportacion(importId, archivoTemporal, mapeosCsv);
        } finally {
          borrarArchivoTemporal(archivoTemporal);
        }
      });
    } catch (RejectedExecutionException colaLLena) {
      // Si no hay lugar para procesarla, no puede quedar un reporte fantasma "en progreso".
      reportesImportacion.remove(importId);
      borrarArchivoTemporal(archivoTemporal);
      throw colaLLena;
    }
    return importId;
  }

  private void borrarArchivoTemporal(Path archivo) {
    try {
      Files.deleteIfExists(archivo);
    } catch (IOException e) {
      log.warn("No se pudo borrar el archivo temporal de importación {}: {}", archivo, e.getMessage());
    }
  }

  private void procesarImportacion(UUID importId, Path archivo, List<MapeoCSV> mapeosCsv) {
    List<String> errores = new ArrayList<>();
    int totalFilas = 0;
    AtomicInteger exitosos = new AtomicInteger();
    List<IDDTO> perfilesPendientes = new ArrayList<>();

    try {
      try (InputStream is = Files.newInputStream(archivo)) {
        LectorCSV<Donante> lector = new LectorCSV<>(',', new PersonaDonanteFilaConverter(mapeosCsv));
        ResultadoLectura<Donante> resultado = lector.importar(is, donante -> {
          try {
            persistirConNotificacion(donante);
            perfilesPendientes.add(perfilDe(donante));
            exitosos.incrementAndGet();
            if (perfilesPendientes.size() >= TAMANO_LOTE_PERFILES) {
              crearPerfilesEnLote(new ArrayList<>(perfilesPendientes), errores);
              perfilesPendientes.clear();
            }
          } catch (Exception e) {
            throw new RuntimeException(
                "Donante '" + donante.getPersona().getNombreDeUsuario() + "': " + e.getMessage(), e);
          }
        });
        totalFilas = resultado.getTotalFilas();
        errores.addAll(resultado.getErrores());
        crearPerfilesEnLote(new ArrayList<>(perfilesPendientes), errores);
        perfilesPendientes.clear();
      }
    } catch (Exception e) {
      errores.add("Error inesperado al procesar el archivo: " + e.getMessage());
    } finally {
      // Si la lectura falló a mitad, no perder los perfiles de las filas ya persistidas.
      if (!perfilesPendientes.isEmpty()) {
        try {
          crearPerfilesEnLote(perfilesPendientes, errores);
        } catch (RuntimeException e) {
          errores.add("No se pudieron enviar " + perfilesPendientes.size()
              + " perfiles pendientes a incentivos: " + e.getMessage());
        }
      }
      // El reporte se cierra siempre: ni un Error que mate al worker puede dejarlo
      // "EN_PROGRESO" para siempre.
      reportesImportacion.put(importId,
          ReporteImportacionDTO.completado(importId, totalFilas, exitosos.get(), errores));
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

  @Transactional(readOnly = true)
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
    LocalDate hoy = LocalDate.now();
    List<Donante> inactivos = repositorioDonantes.buscarInactivosParaNotificar(
        hoy.minusDays(20), PageRequest.of(0, TAMANO_LOTE_INACTIVIDAD));
    for (Donante p : inactivos) {
      try {
        fabricaEstrategias.ejecutar(TipoEventoNotificacion.INACTIVIDAD_PERSONA_DONANTE, p);
        p.setUltimaNotificacionInactividad(hoy);
      } catch (Exception e) {
        log.warn("No se pudo notificar la inactividad del donante {}: {}", p.getId(), e.getMessage());
      }
    }
  }

  // Resuelve (o crea) la Ciudad/Provincia/Pais del catálogo geográfico compartido ANTES de
  // construir el domain object, para evitar que Direccion apunte a una Ciudad nunca persistida
  // (eso rompía merge() con EntityNotFoundException, ver RepositorioDeCiudades / EntidadBeneficiariaService).
  private Ciudad resolverCiudad(DireccionDTO direccionDTO) {
    if (direccionDTO == null) return null;
    return gestorCatalogo.obtenerOCrearCiudad(direccionDTO.getPais(), direccionDTO.getProvincia(), direccionDTO.getCiudad());
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
