package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.*;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Ciudad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Pais;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Provincia;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.*;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.*;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EntregaService {

  private static final Logger log = LoggerFactory.getLogger(EntregaService.class);

  private final RepositorioItemEntrega repoItemEntrega;
  private final RepositorioRutas repoRutas;
  private final RepositorioEntidades repoEntidades;
  private final RepositorioDirecciones repoDirecciones;
  private final RepositorioCiudades repoCiudades;
  private final RepositorioProvincias repoProvincias;
  private final RepositorioPaises repoPaises;
  private final RepositorioUnidadesDeMedida repoUnidades;
  private final GestorPublicacionEventos gestorPublicacionEventos;

  /** Se usa para abrir transacciones propias, aisladas de la del listener. */
  private final PlatformTransactionManager gestorTransacciones;

  public EntregaService(RepositorioItemEntrega repoItemEntrega,
                        RepositorioRutas repoRutas,
                        RepositorioEntidades repoEntidades,
                        RepositorioDirecciones repoDirecciones,
                        RepositorioCiudades repoCiudades,
                        RepositorioProvincias repoProvincias,
                        RepositorioPaises repoPaises,
                        RepositorioUnidadesDeMedida repoUnidades,
                         GestorPublicacionEventos gestorPublicacionEventos,
                         PlatformTransactionManager gestorTransacciones) {
      this.repoItemEntrega = repoItemEntrega;
      this.repoRutas = repoRutas;
      this.repoEntidades = repoEntidades;
      this.repoDirecciones = repoDirecciones;
      this.repoCiudades = repoCiudades;
      this.repoProvincias = repoProvincias;
      this.repoPaises = repoPaises;
      this.repoUnidades = repoUnidades;
      this.gestorPublicacionEventos = gestorPublicacionEventos;
      this.gestorTransacciones = gestorTransacciones;
  }

  @PostConstruct
  public void inicializarCatalogoUnidades() {
    try {
      List<UnidadDeMedida> defaultUnits = List.of(
              UnidadDeMedida.UNIDADES,
              UnidadDeMedida.KILOGRAMOS,
              UnidadDeMedida.LITROS
      );
      for (UnidadDeMedida unidad : defaultUnits) {
        if (!repoUnidades.existsById(unidad.getIdUnidad())) {
          repoUnidades.save(unidad);
        }
      }
    } catch (Exception e) {
      log.warn("No se pudo pre-cargar el catálogo de unidades de medida: {}", e.getMessage());
    }
  }

  // --- MÉTODOS CRUD BÁSICOS ---
  public BienesDTO findAll() {
      List<ItemEntrega> items = repoItemEntrega.findAll();
      return new BienesDTO(items.stream().map(ItemEntrega::getIdDonacion).toList() , convertirItemsADTO(items));
  }

  /**
   * Un ítem de entrega por su id de donación. Devuelve el DTO y no la entidad: Jackson seguiría
   * los getters de la entidad y entraría en ciclo al serializar.
   */
  public BienDTO findById(UUID id) {
    Optional<ItemEntrega> item = repoItemEntrega.findById(id);
    if (item.isEmpty()){
      throw new IllegalArgumentException("Entrega no encontrada");
    }
    DireccionDTO direccionDTO = null;
    if (item.get().getEntidadDestino() != null && item.get().getEntidadDestino().getDireccionDestino() != null) {
      var dir = item.get().getEntidadDestino().getDireccionDestino();
      var ciudad = dir.getCiudad();
      var provincia = ciudad != null ? ciudad.getProvincia() : null;
      var pais = provincia != null ? provincia.getPais() : null;
      direccionDTO = new DireccionDTO(
          item.get().getEntidadDestino().getIdEntidadBeneficiaria(),
          dir.getCalle1(),
          dir.getCalle2(),
          dir.getAltura(),
          dir.getPiso(),
          dir.getDepartamento(),
          ciudad != null ? ciudad.getNombre() : null,
          provincia != null ? provincia.getNombre() : null,
          pais != null ? pais.getNombre() : null
      );
    }
    List<EventoLogisticaDTO> eventosDTO = item.get().getEventos() != null
        ? item.get().getEventos().stream().map(evento -> new EventoLogisticaDTO(evento.getId(), evento.getTipoEvento(), evento.getReferenciaId(), evento.getJustificacion(), evento.getPayloadJson())).toList()
        : List.of();

    return new BienDTO(
        item.get().getCantidad(),
        item.get().getUnidad() != null ? item.get().getUnidad().toString() : null,
        item.get().getEstado() != null ? item.get().getEstado().toString() : null,
        item.get().getFechaCambioEstado(),
        item.get().getFotoComprobante(),
        direccionDTO,
        eventosDTO
    );
  }

  /**
   * Borra el item de entrega. El {@code findById} previo no evita nada: {@code deleteById} ya es
   * no-op si no existe. Está para devolver el 404 que documenta el controller.
   */
  public void delete(UUID id) {
    Optional<ItemEntrega> item = repoItemEntrega.findById(id);
    if(item.isEmpty()){
      throw new IllegalArgumentException("Entrega no encontrada");
    }
    repoItemEntrega.deleteById(id);
  }

  // --- MÉTODOS DE NEGOCIO ---

  /**
 * Registra los items de entrega de una donacion que llega por el broker.
 *
 * <p>Es idempotente porque RabbitMQ es de entrega al menos una vez: la guarda de existencia por
 * {@code idDonacion} evita que una redelivery vuelva a pasar por {@code save()}, que como la
 * clave es natural sin {@code @GeneratedValue} siempre va por {@code merge()} y reescribiría la
 * fila con {@code estado = PENDIENTE}.
 */
  public void procesarPeticion(EntregaDTO request) {
    if (request == null) {
      throw new IllegalArgumentException("La petición no puede ser nula");
    }

    if (request.getDonacionResumen() == null) {
      throw new IllegalArgumentException("La petición no trae el resumen de la donación");
    }
    if (request.getEntidadBeneficiaria() == null) {
      throw new IllegalArgumentException("La petición no trae la entidad beneficiaria");
    }

    List<BienDTO> bienes = request.getDonacionResumen().getBienes();
    List<UUID> idsDonaciones = request.getDonacionResumen().getIdsDonaciones();

    if (bienes == null) {
      throw new IllegalArgumentException("La lista de bienes de la donación no puede ser nula");
    }
    if (idsDonaciones == null) {
      throw new IllegalArgumentException("La lista de IDs de donaciones no puede ser nula");
    }

    if (bienes.size() != idsDonaciones.size()) {
      throw new IllegalArgumentException("La cantidad de bienes (" + bienes.size()
              + ") no coincide con la cantidad de donaciones (" + idsDonaciones.size() + ")");
    }

    // El catálogo va en su propia transacción: que dos instancias lo escriban a la vez es normal y
    // no puede arrastrar al trabajo real.
    Entidad entidadDestino = resolverEntidad(request.getEntidadBeneficiaria());

    // Los ítems sí van en una transacción: o se registran todos los bienes del mensaje, o
    // ninguno. Registrar la mitad dejaría donaciones partidas.
    int[] conteo = itemsEnUnaTransaccion(bienes, idsDonaciones, entidadDestino);

    log.info("Petición procesada: {} ítems registrados, {} repetidos omitidos", conteo[0],
            conteo[1]);
  }

  /**
 * Resuelve la entidad beneficiaria en su propia transaccion, tolerando la carrera entre
 * instancias. Solo {@code Entidad} puede colisionar: es la única con clave natural, y su
 * {@code idEntidad} viene del mensaje.
 */
  private Entidad resolverEntidad(DireccionDTO dto) {

    if (dto == null) {
      throw new IllegalArgumentException(
              "La entidad beneficiaria no trae dirección");
    }

    UUID idEntidad = dto.getIdEntidad();

    if (idEntidad == null) {
      throw new IllegalArgumentException(
              "El ID de la entidad beneficiaria no puede ser nulo");
    }

    return enSuPropiaTransaccion(() -> {

      Optional<Entidad> yaExistente =
              repoEntidades.findById(idEntidad);

      if (yaExistente.isPresent()) {
        return yaExistente.get();
      }

      Direccion direccion = resolverDireccion(dto);

      try {
        return repoEntidades.save(
                new Entidad(idEntidad, direccion));

      } catch (DataIntegrityViolationException carrera) {

        log.info(
                "La entidad {} ya fue registrada por otra instancia",
                idEntidad);

        return repoEntidades.findById(idEntidad)
                .orElseThrow(() -> new IllegalStateException(
                        "La entidad " + idEntidad
                                + " no se pudo resolver tras una carrera",
                        carrera));
      }
    });
  }

  private Direccion resolverDireccion(DireccionDTO dto) {

    // 1. Buscar el país o crearlo si no existe.
    Pais pais = repoPaises.findByNombreIgnoreCase(dto.getPais())
            .orElseGet(() -> {
              Pais nuevoPais = new Pais();
              nuevoPais.setNombre(dto.getPais());
              return repoPaises.save(nuevoPais);
            });

    // 2. Buscar la provincia dentro de ese país o crearla.
    Provincia provincia = repoProvincias
            .findByNombreIgnoreCaseAndPais_IdPais(
                    dto.getProvincia(), pais.getIdPais())
            .orElseGet(() -> {
              Provincia nuevaProvincia = new Provincia();
              nuevaProvincia.setNombre(dto.getProvincia());
              nuevaProvincia.setPais(pais);
              return repoProvincias.save(nuevaProvincia);
            });

    // 3. Buscar la ciudad dentro de esa provincia o crearla.
    Ciudad ciudad = repoCiudades
            .findByNombreIgnoreCaseAndProvincia_IdProvincia(
                    dto.getCiudad(), provincia.getIdProvincia())
            .orElseGet(() -> {
              Ciudad nuevaCiudad = new Ciudad(
                      dto.getCiudad(), provincia);
              return repoCiudades.save(nuevaCiudad);
            });

    // 4. Construir la dirección usando el catálogo resuelto.
    Direccion direccion = new Direccion();
    direccion.setIdDireccion(UUID.randomUUID());
    direccion.setCalle1(dto.getCalleUno());
    direccion.setCalle2(dto.getCalleDos());
    direccion.setAltura(dto.getAltura());
    direccion.setPiso(dto.getPiso());
    direccion.setDepartamento(dto.getDepartamento());
    direccion.setCiudad(ciudad);

    return repoDirecciones.save(direccion);
  }

  /**
   * Registra los items del mensaje en una sola transaccion.
   *
   * @return un arreglo con {registrados, repetidos}
   */
  private int[] itemsEnUnaTransaccion(List<BienDTO> bienes, List<UUID> idsDonaciones,
                                      Entidad entidadDestino) {
    return enSuPropiaTransaccion(() -> {
      int registrados = 0;
      int repetidos = 0;

      for (int j = 0; j < bienes.size(); j++) {
        UUID idDonacion = idsDonaciones.get(j);

        // Un `continue` y no un `return` porque la donacion puede traer bienes nuevos junto con
        // otros ya registrados.
        if (repoItemEntrega.existsById(idDonacion)) {
          repetidos++;
          log.info("La donacion {} ya estaba registrada, el mensaje se repite y se omite",
                  idDonacion);
          continue;
        }

        BienDTO bien = bienes.get(j);

        UnidadDeMedida unidadDominio = mapearUnidadDeMedida(bien.getUnidadDeMedida());

        ItemEntrega nuevoItem = new ItemEntrega(idDonacion, bien.getCantidad(), unidadDominio,
                entidadDestino);
        repoItemEntrega.save(nuevoItem);
        registrados++;
      }

      return new int[] {registrados, repetidos};
    });
  }

  /**
 * Corre un bloque en una transaccion propia, independiente de la que tenga abierta el caller.
 *
 * <p>{@code REQUIRES_NEW} y no el {@code @Transactional} del metodo porque hace falta que el
 * rollback del catalogo <b>no</b> se lleve por delante los items.
 */
  private <T> T enSuPropiaTransaccion(java.util.function.Supplier<T> bloque) {
    org.springframework.transaction.support.TransactionTemplate plantilla =
            new org.springframework.transaction.support.TransactionTemplate(gestorTransacciones);
    plantilla.setPropagationBehavior(
            org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return plantilla.execute(estado -> bloque.get());
  }

  /**
   * Mapea el String recibido desde el DTO a la instancia estática correspondiente del dominio.
   */
  private UnidadDeMedida mapearUnidadDeMedida(String unidadStr) {
    if (unidadStr == null) {
      throw new IllegalArgumentException("La unidad de medida no puede ser nula");
    }
      return switch (unidadStr.toUpperCase()) {
          case "UNIDADES", "UNIDAD" -> UnidadDeMedida.UNIDADES;
          case "KILOGRAMOS", "KG" -> UnidadDeMedida.KILOGRAMOS;
          case "LITROS", "L" -> UnidadDeMedida.LITROS;
          default -> throw new IllegalArgumentException("Unidad de medida no soportada: " + unidadStr);
      };
  }

  /**
   * Cambia el estado de un item. Es la unica via por la que el estado se mueve.
   *
   * <p>Transaccional porque publica el evento de trazabilidad y despues guarda el item: sin
   * transaccion, un {@code save} fallado dejaba el evento afuera y donaciones-service se enteraba
   * de una entrega que en la base no ocurrio. El {@code @Version} del {@code ItemEntrega} hace
   * que dos operadores que confirmen la misma entrega al mismo tiempo no se pisen.
   */
  @Transactional
  public void actualizarEstado(UUID idDonacion, ActualizacionEntregaDTO request) {
    ItemEntrega item = repoItemEntrega.findById(idDonacion)
            .orElseThrow(() -> new IllegalArgumentException("Donación no encontrada con el ID proporcionado"));

    if (request.getEstado() == null) {
      throw new IllegalArgumentException("El estado no puede ser nulo");
    }

    switch (request.getEstado().toUpperCase()) {
      case "ENTREGADA":
        if(faltaTexto(request.getFotoUrl())) {
          throw new IllegalArgumentException("Se requiere una foto para confirmar la entrega exitosa.");
        }
        Ruta rutaEntregada = repoRutas.findByIdDonacion(item.getIdDonacion())
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la ruta correspondiente a la donación " + idDonacion));
        repoItemEntrega.save(gestorPublicacionEventos.publicarEntregaConfirmada(item, rutaEntregada, request.getFotoUrl()));
        sincronizarItemEnRuta(rutaEntregada, item);
        break;

      case "NO_RECIBIDA":
        if(faltaTexto(request.getJustificacion())) {
          throw new IllegalArgumentException("Se requiere justificar el motivo por el cual falló la entrega.");
        }
        Ruta rutaFallida = repoRutas.findByIdDonacion(item.getIdDonacion())
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la ruta correspondiente a la donación " + idDonacion));
        repoItemEntrega.save(gestorPublicacionEventos.publicarEntregaFallida(item, rutaFallida, request.getJustificacion()));
        sincronizarItemEnRuta(rutaFallida, item);
        break;

      case "PENDIENTE":
        // Reingreso a depósito tras revisión de una entrega NO_RECIBIDA.
        // reingresarADeposito() ya valida que solo se pueda hacer desde NO_RECIBIDA.
        repoItemEntrega.save(gestorPublicacionEventos.publicarReingresoDeposito(item));
        repoRutas.findByIdDonacion(item.getIdDonacion()).ifPresent(ruta -> sincronizarItemEnRuta(ruta, item));
        break;

      default:
        throw new IllegalArgumentException("Estado no válido. Use ENTREGADA, NO_RECIBIDA o PENDIENTE.");
    }

    repoItemEntrega.save(item);
  }

  private void sincronizarItemEnRuta(Ruta ruta, ItemEntrega item) {
    if (ruta != null && ruta.getParadas() != null) {
      for (Parada parada : ruta.getParadas()) {
        if (parada.getItems() != null) {
          for (ItemEntrega it : parada.getItems()) {
            if (item.getIdDonacion().equals(it.getIdDonacion())) {
              it.setEstado(item.getEstado());
              it.setFotoComprobante(item.getFotoComprobante());
              it.setFechaCambioEstado(item.getFechaCambioEstado());
              it.setEventos(item.getEventos());
            }
          }
        }
      }
      repoRutas.save(ruta);
    }
  }

  /** {@code true} si el texto vino nulo o vacío. */
  private boolean faltaTexto(String elemento){
    return (elemento == null || elemento.trim().isEmpty());
  }

  /**
   * Ítems en estado NO_RECIBIDA, pendientes de revisión (reingreso a depósito
   * o replanificación). El control de quién puede llamar a este endpoint
   * es responsabilidad del front/capa de autorización, no de este servicio.
   */
  public BienesDTO obtenerEntregasNoRecibidas() {
    List<ItemEntrega> items = repoItemEntrega.findByEstado(EstadoEntrega.NO_RECIBIDA);
    return new BienesDTO(items.stream().map(ItemEntrega::getIdDonacion).toList() , convertirItemsADTO(items));
  }

  private List<BienDTO> convertirItemsADTO(List<ItemEntrega> items){
    return items.stream().map(this::convertirABienDTO).toList();
  }

  private BienDTO convertirABienDTO(ItemEntrega item){
    return new BienDTO(item.getCantidad(), item.getUnidad().getNombre(), item.getEstado().toString(), item.getFechaCambioEstado(), item.getFotoComprobante(), convertirADireccionDTO(item.getEntidadDestino()), convertirEventosADTO(item.getEventos()));
  }

  private Direccion convertirDireccionDTO(DireccionDTO dto) {
    if (dto == null) return null;
    return new Direccion(
        dto.getCalleUno(), dto.getCalleDos(), dto.getAltura(),
        dto.getPiso(), dto.getDepartamento(), dto.getCiudad(),
        dto.getProvincia(), dto.getPais()
    );
  }

  private DireccionDTO convertirADireccionDTO(Entidad entidad){
    return new DireccionDTO(entidad.getIdEntidadBeneficiaria(), entidad.getDireccionDestino().getCalle1(), entidad.getDireccionDestino().getCalle2(), entidad.getDireccionDestino().getAltura(), entidad.getDireccionDestino().getPiso(), entidad.getDireccionDestino().getDepartamento(), entidad.getDireccionDestino().getCiudad().getNombre(), entidad.getDireccionDestino().getCiudad().getProvincia().getNombre(), entidad.getDireccionDestino().getCiudad().getProvincia().getPais().getNombre());
  }

  private List<EventoLogisticaDTO> convertirEventosADTO(List<EventoLogistica> eventos){
    if(eventos != null){
      return eventos.stream().map(this::convertirAEventoDTO).toList();
    }
    return new ArrayList<>();
  }

  private EventoLogisticaDTO convertirAEventoDTO(EventoLogistica evento){
    return new EventoLogisticaDTO(evento.getId(), evento.getTipoEvento(), evento.getReferenciaId(), evento.getJustificacion(), evento.getPayloadJson());
  }
}
