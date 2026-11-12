package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.BienResumenDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.direccion.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.entrega.BienDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.entrega.EntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.messaging.ProductorLogistica;
import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.ResultadoMatchmakingDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personadonante.FormularioRequestDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personadonante.FormularioResumenDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AsignadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadRecurrente;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.*;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DonacionService {
  private static final Logger log = LoggerFactory.getLogger(DonacionService.class);

  private final GestorAsignaciones gestorAsignaciones;
  private final GestorFormulario gestorFormulario;
  private final GestorMatchmaking gestorMatchmaking;
  private final GestorCatalogo gestorCatalogo;
  private final RepositorioDeDonaciones repositorioDonaciones;
  private final RepositorioDeDonantes repositorioDonantes;
  private final RepositorioDeFormularios repositorioFormularios;
  private final RepositorioDeEntidadesBeneficiarias repositorioEntidadesBeneficiarias;
  private final RepositorioDeResultadosMatchmaking repositorioDeResultadosMatchmaking;
  private final RepositorioDeBienes repositorioBienes;
  private final ProductorLogistica productorLogistica;
  private final TransactionTemplate requiereNuevaTransaccion;

  public DonacionService(GestorAsignaciones gestorAsignaciones,
                         GestorFormulario gestorFormulario, GestorMatchmaking gestorMatchmaking,
                         GestorCatalogo gestorCatalogo,
                         RepositorioDeDonaciones repositorioDonaciones,
                         RepositorioDeDonantes repositorioDonantes,
                         RepositorioDeFormularios repositorioFormularios,
                         RepositorioDeEntidadesBeneficiarias repositorioEntidadesBeneficiarias,
                         RepositorioDeResultadosMatchmaking repositorioDeResultadosMatchmaking,
                         RepositorioDeBienes repositorioBienes,
                         ProductorLogistica productorLogistica,
                         PlatformTransactionManager transactionManager) {
    this.requiereNuevaTransaccion = new TransactionTemplate(transactionManager);
    this.requiereNuevaTransaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.gestorAsignaciones = gestorAsignaciones;
    this.gestorFormulario = gestorFormulario;
    this.gestorMatchmaking = gestorMatchmaking;
    this.gestorCatalogo = gestorCatalogo;
    this.repositorioDonaciones = repositorioDonaciones;
    this.repositorioDonantes = repositorioDonantes;
    this.repositorioFormularios = repositorioFormularios;
    this.repositorioEntidadesBeneficiarias = repositorioEntidadesBeneficiarias;
    this.repositorioDeResultadosMatchmaking = repositorioDeResultadosMatchmaking;
    this.repositorioBienes = repositorioBienes;
    this.productorLogistica = productorLogistica;
  }

  @Transactional(readOnly = true)
  public List<DonacionDTO> obtenerTodas() {
    return repositorioDonaciones.obtenerTodos().stream()
                           .map(DonacionDTO::from).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<DonacionDTO> obtenerDeDonante(UUID donanteId) {
    return repositorioDonaciones.buscarPorDonante(donanteId).stream()
            .map(DonacionDTO::from).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public Page<DonacionDTO> obtenerTodas(Pageable pageable) {
    return repositorioDonaciones.obtenerTodos(pageable).map(DonacionDTO::from);
  }

  @Transactional(readOnly = true)
  public DonacionDTO obtenerPorId(UUID id) {
    return DonacionDTO.from(repositorioDonaciones.obtenerPorId(id).orElseThrow(() -> new EntityNotFoundException("No se encontró la donación con ID: " + id)));
  }

  @Transactional(rollbackFor = Exception.class)
  public List<DonacionDTO> procesarFormulario(FormularioRequestDTO request) {
    Donante donante = repositorioDonantes.buscarPorId(request.getIdDonante()).orElse(null);
    if (donante == null) throw new IllegalArgumentException("No se encontró persona con ese ID");

    List<Bien> bienesNormal = request.getBienes() != null ? resolverBienes(request.getBienes()) : List.of();
    bienesNormal.forEach(this::crearBien);

    Formulario formularioGenerado = new Formulario(donante, bienesNormal, request.getFechaRealizacion());
    repositorioFormularios.guardar(formularioGenerado);
    List<Donacion> donacionesProcesadas = gestorFormulario.procesarFormulario(formularioGenerado);

    repositorioDonaciones.guardarDonaciones(donacionesProcesadas);
    repositorioDonantes.agregarFormularioADonante(donante.getId(), formularioGenerado);

    return donacionesProcesadas.stream().map(DonacionDTO::from).collect(Collectors.toList());
  }

  // Ver FormularioResumenDTO: antes no había forma de listar/borrar Formularios vía API, lo que
  // dejaba a cualquier Donante que hubiera donado imposible de borrar (409 por FK donante_id).
  @Transactional(readOnly = true)
  public List<FormularioResumenDTO> obtenerFormularios() {
    return repositorioFormularios.obtenerTodos().stream()
                           .map(FormularioResumenDTO::from).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public Page<FormularioResumenDTO> obtenerFormularios(Pageable pageable) {
    return repositorioFormularios.obtenerTodos(pageable).map(FormularioResumenDTO::from);
  }

  @Transactional(rollbackFor = Exception.class)
  public void eliminarFormulario(UUID id) {
    repositorioFormularios.eliminarPorId(id);
  }

  /**
   * Cada donación se procesa en su propia transacción (REQUIRES_NEW): un fallo de integridad
   * en una no tumba el lote completo por rollback-only (punto 74).
   */
  public void ejecutarMatchmakingADemanda() {
    List<UUID> ids = repositorioDonaciones.buscarIdsSinAsignar();
    if (ids.isEmpty()) {
      return;
    }
    Map<UUID, Integer> conteosPorEntidad = repositorioDonaciones
        .contarEntregadasUltimoTrimestrePorEntidad(LocalDate.now().minusMonths(3));
    Map<UUID, List<RepositorioDeDonaciones.EntregaNecesidad>> entregasPorNecesidad =
        repositorioDonaciones.entregasPorNecesidadYFecha();
    for (UUID id : ids) {
      try {
        requiereNuevaTransaccion.executeWithoutResult(
            estado -> procesarMatchmakingDeDonacion(id, conteosPorEntidad, entregasPorNecesidad));
      } catch (Exception e) {
        log.error("Error al procesar matchmaking para la donación {}: {}", id, e.getMessage());
      }
    }
  }

  private void procesarMatchmakingDeDonacion(UUID donacionId,
                                             Map<UUID, Integer> conteosPorEntidad,
                                             Map<UUID, List<RepositorioDeDonaciones.EntregaNecesidad>> entregasPorNecesidad) {
    Donacion donacion = repositorioDonaciones.obtenerPorId(donacionId).orElse(null);
    if (donacion == null) {
      return;
    }
    List<EntidadBeneficiaria> entidades = repositorioEntidadesBeneficiarias.obtenerTodasActivas();
    Map<UUID, Integer> cantidadesVigentes = calcularCantidadesVigentes(entidades, entregasPorNecesidad);
    new AsignadorDonaciones(gestorMatchmaking, gestorAsignaciones,
            repositorioDeResultadosMatchmaking, conteosPorEntidad, cantidadesVigentes)
            .procesarMatchmaking(donacion, entidades);
  }

  /** Progreso por necesidad sin navegar donaciones: usa las filas agregadas por fecha. */
  private Map<UUID, Integer> calcularCantidadesVigentes(
      List<EntidadBeneficiaria> entidades,
      Map<UUID, List<RepositorioDeDonaciones.EntregaNecesidad>> entregasPorNecesidad) {
    LocalDate hoy = LocalDate.now();
    Map<UUID, Integer> cantidades = new HashMap<>();
    for (EntidadBeneficiaria entidad : entidades) {
      for (Necesidad necesidad : entidad.getNecesidades()) {
        if (!necesidad.isActiva()) {
          continue;
        }
        List<RepositorioDeDonaciones.EntregaNecesidad> entregas =
            entregasPorNecesidad.getOrDefault(necesidad.getId(), List.of());
        int cantidad = 0;
        if (necesidad instanceof NecesidadRecurrente recurrente) {
          int plazo = recurrente.getPlazoEnDias() != null ? recurrente.getPlazoEnDias() : 30;
          LocalDate desde = hoy.minusDays(plazo);
          for (RepositorioDeDonaciones.EntregaNecesidad entrega : entregas) {
            if (entrega.fechaEntrega() != null && entrega.fechaEntrega().isAfter(desde)) {
              cantidad += entrega.cantidad();
            }
          }
        } else {
          for (RepositorioDeDonaciones.EntregaNecesidad entrega : entregas) {
            cantidad += entrega.cantidad();
          }
        }
        cantidades.put(necesidad.getId(), cantidad);
      }
    }
    return cantidades;
  }

  // A diferencia de dto.toDomain() (que sólo completa id/descripcion/bienes, dejando
  // donante/entidad/estado/subcategoria/fechaEntrega en null), acá partimos de la Donacion
  // existente y sólo pisamos los campos que vienen en el DTO. Con persistencia real (merge()),
  // guardar un objeto mayormente-null hubiera nuleado esas columnas en la base.
  @Transactional(rollbackFor = Exception.class)
  public DonacionDTO actualizarDonacion(UUID id, DonacionDTO dto) {
    Donacion existente = repositorioDonaciones.obtenerPorId(id)
            .orElseThrow(() -> new EntityNotFoundException("Donación no encontrada con ID: " + id));

    if (dto.getDescripcion() != null) {
      existente.setDescripcion(dto.getDescripcion());
    }
    if (dto.getBienes() != null) {
      // Los bienes que la lista deja afuera no se borran (ver RepositorioDeBienes: la fila es
      // compartida con Formulario.donaciones) ni se cuentan como entregados, así que reescribir
      // el contenido de una donación ya asignada/entregada dejaría los conteos de
      // Necesidad.cantidadRecibida() sin correspondencia con lo realmente entregado. Por eso el
      // PUT sólo se permite mientras la donación sigue en depósito.
      if (existente.getEstado() != Estado.EN_DEPOSITO) {
        throw new IllegalArgumentException(
                "No se pueden modificar los bienes de una donación que no está en depósito (estado actual: " + existente.getEstado() + ")");
      }
      // Los bienes nuevos NO se guardan acá uno por uno: Bien.id se inicializa en el campo
      // (UUID.randomUUID()), así que para Spring Data nunca son "nuevos" y save() termina en
      // em.merge(), que deja en la sesión una copia administrada aparte de la instancia que
      // guardamos en la colección. Al flushear una Donacion ya administrada, Hibernate se
      // encontraba con las dos y cortaba con NonUniqueObjectException ("A different object with
      // the same identifier value was already associated with the session"). Donacion.bienes
      // cascadea PERSIST/MERGE, así que el insert sale solo al guardar la donación.
      existente.setBienes(resolverBienes(dto.getBienes()));
    }

    return DonacionDTO.from(repositorioDonaciones.actualizar(existente.getId(), existente).get());
  }

  @Transactional(rollbackFor = Exception.class)
  public void eliminarDonacion(UUID id) {
    // Punto 54: el resultado de matchmaking referencia la donación por FK y no tiene
    // cascade REMOVE. Se borra primero para que el DELETE no termine en 409.
    repositorioDeResultadosMatchmaking.findByDonacionId(id)
        .ifPresent(repositorioDeResultadosMatchmaking::eliminarResultado);
    repositorioDonaciones.eliminarPorId(id);
  }

  @Transactional(rollbackFor = Exception.class)
  public DonacionDTO cambiarEstado(UUID id, String nuevoEstado, String justificacion) {
    return DonacionDTO.from(gestorAsignaciones.cambiarEstado(id, nuevoEstado, justificacion));
  }

  @Transactional(rollbackFor = Exception.class)
  public DonacionDTO marcarComoVencida(UUID id) {
    return DonacionDTO.from(gestorAsignaciones.cambiarEstado(id, "VENCIDO", "Registrado como vencido por la administración."));
  }

  @Transactional(readOnly = true)
  public List<ResultadoMatchmakingDTO> obtenerTodosLosResultadosMatchmaking() {
    return repositorioDeResultadosMatchmaking.findAll().stream()
                            .map(ResultadoMatchmakingDTO::from).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public Page<ResultadoMatchmakingDTO> obtenerTodosLosResultadosMatchmaking(Pageable pageable) {
    return repositorioDeResultadosMatchmaking.findAll(pageable).map(ResultadoMatchmakingDTO::from);
  }

  @Transactional(rollbackFor = Exception.class)
  public void asignarPropuesta(UUID donacionId, Integer posicion) {
    PropuestaAsignacion propuestaAsignacion = gestorMatchmaking.obtenerPropuestaSeleccionadaParaDonacion(donacionId, posicion);
    Donacion donacion = repositorioDonaciones.obtenerPorId(donacionId).orElseThrow(() -> new IllegalArgumentException("No se encontró la donación"));
    gestorAsignaciones.asignarPropuesta(donacion, propuestaAsignacion);
    eliminarResultadoMatchmaking(donacion.getId());
    gestorAsignaciones.cambiarEstado(donacion.getId(), "ASIGNADO", "Donacion Asignada");
    gestorAsignaciones.cambiarEstado(donacion.getId(), "LISTA_PARA_ENTREGAR", "Lista para entregar");

    Runnable publicarLogistica = () -> {
      try {
        publicarEntregaALogistica(donacionId);
      } catch (Exception e) {
        log.error("No se pudo publicar la entrega a logística para la donación {}: {}", donacionId, e.getMessage(), e);
      }
    };

    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          publicarLogistica.run();
        }
      });
    } else {
      publicarLogistica.run();
    }
  }

  /** Publica el item de entrega: los ids de donación, los bienes agregados por unidad y la
   *  dirección de la entidad. Es todo lo que logística necesita para crear los ítems de entrega;
   *  por contrato, no consulta este servicio para nada más. */
  private void publicarEntregaALogistica(UUID donacionId) {
    // afterCommit no tiene sesión: el armado del DTO (que toca bienes lazy) va en una
    // transacción nueva y la publicación queda afuera.
    EntregaDTO entrega = requiereNuevaTransaccion.execute(
            estado -> armarEntregaParaLogistica(donacionId));
    if (entrega != null) {
      productorLogistica.publicarDonacionAsignada(entrega);
    }
  }

  private EntregaDTO armarEntregaParaLogistica(UUID donacionId) {
    Donacion donacion = repositorioDonaciones.obtenerPorId(donacionId)
            .orElseThrow(() -> new IllegalArgumentException("No se encontro la donacion"));

    EntidadBeneficiaria entidad = donacion.getEntidad();
    if (entidad == null || entidad.getDireccion() == null) {
      return null;
    }

    Map<UnidadDeMedida, Integer> cantidadesPorUnidad = new LinkedHashMap<>();
    donacion.getBienes().forEach(b ->
            cantidadesPorUnidad.merge(b.getUnidadUtilizada(),
                    b.getPeso() != null ? b.getPeso() : 0, Integer::sum));

    List<BienDTO> bienes = new ArrayList<>();
    List<UUID> idsParaLogistica = new ArrayList<>();

    cantidadesPorUnidad.forEach((unidad, cantidad) -> {
        bienes.add(new BienDTO(cantidad, unidad != null ? unidad.name() : null));
        idsParaLogistica.add(donacion.getId());
    });

    DireccionDTO direccionEntidad = DireccionDTO.from(entidad.getDireccion());
    direccionEntidad.setIdEntidad(entidad.getId());

    return new EntregaDTO(idsParaLogistica, bienes, direccionEntidad);
  }


  private Bien resolverBien(BienResumenDTO dto) {
    SubcategoriaBien subcategoria = gestorCatalogo.obtenerOCrearSubcategoria(dto.getCategoria(), dto.getSubcategoria());
    return dto.toDomain(subcategoria);
  }

  private List<Bien> resolverBienes(List<BienResumenDTO> bienesDto) {
    List<Bien> resultado = new ArrayList<>();
    for (int i = 0; i < bienesDto.size(); i++) {
      try {
        resultado.add(resolverBien(bienesDto.get(i)));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Bien en la posición " + i + ": " + e.getMessage());
      }
    }
    return resultado;
  }

  private void crearBien(Bien nuevoBien) {
    try {
      repositorioBienes.guardar(nuevoBien);
      System.out.println("Bien registrado con éxito con ID: " + nuevoBien.getId());
    } catch (IllegalArgumentException e) {
      System.err.println("Error al registrar bien: " + e.getMessage());
    }
  }

  private void eliminarResultadoMatchmaking(UUID donacionId){
    ResultadoMatchmaking resultado = repositorioDeResultadosMatchmaking.findByDonacionId(donacionId).orElseThrow(() -> new IllegalArgumentException(
                    "No hay resultado de matchmaking para la donación " + donacionId
            )
    );
    repositorioDeResultadosMatchmaking.eliminarResultado(resultado);
  }
}