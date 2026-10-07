package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.*;
import ar.edu.utn.frba.ddsi.logisticas.dto.evento.EventoLogisticaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.*;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.*;
// El merge movio estos dos a subpaquetes. El wildcard de arriba no los alcanza, asi que van
// explicitos: el servicio los escribe contra findByIdDonacion() y findByEstado(), que solo
// existen en las versiones de subpaquete.
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.items.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.rutas.RepositorioRutas;
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

  // --- MÉTODOS CRUD BÁSICOS ---
  public BienesDTO findAll() {
      List<ItemEntrega> items = repoItemEntrega.findAll();
      return new BienesDTO(items.stream().map(ItemEntrega::getIdDonacion).toList() , convertirItemsADTO(items));
  }

  public ItemEntrega findById(UUID id) {
    return repoItemEntrega.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Entrega no encontrada"));
  }

  public void delete(UUID id) {
    Optional<ItemEntrega> item = repoItemEntrega.findById(id);
    if(item.isPresent()){
      repoItemEntrega.deleteById(id);
      throw new IllegalArgumentException("Entrega no encontrada");
    }
  }

  // --- MÉTODOS DE NEGOCIO ---

  /**
   * Registra los items de entrega de una donacion que llega por el broker.
   *
   * <p><b>Es idempotente, y por una razon que no es obvia.</b> RabbitMQ es de entrega al menos
   * una vez: un mensaje vuelve a llegar si el consumidor procesa pero no llega a hacer el
   * <i>ack</i>, por ejemplo si se cae la conexion o se reinicia la instancia. Con N instancias
   * de logistica consumiendo la cola compartida eso no es teorico, y pasa.
   *
   * <p>Antes de agregar la guarda de existencia, una redelivery <b>no fallaba</b>: el
   * {@code idDonacion} es clave natural sin {@code @GeneratedValue}, asi que el {@code isNew} de
   * Spring Data da {@code false} y {@code save()} va siempre por {@code merge()}, que hace
   * {@code SELECT} y despues {@code UPDATE}. El {@code UPDATE} reescribia la fila con lo que
   * pone el constructor, que es {@code estado = PENDIENTE}, con lo que una entrega ya
   * confirmada volvia a PENDIENTE. Sin error y sin log, que es peor que una duplicacion porque
   * una duplicacion se ve.
   *
   * <p><b>El catalogo se resuelve una vez por mensaje y no por bien.</b> Antes estaba dentro del
   * {@code for}, asi que una donacion con tres bienes escribia la misma direccion tres veces (el
   * punto 15 del backlog), y con dos instancias dos donaciones para la misma entidad se pisaban
   * {@code Pais -> Provincia -> Ciudad -> Direccion} en silencio. Ahora se resuelve una vez, y el
   * {@code @Version} de esas entidades hace que la segunda que llegue reciba
   * {@code OptimisticLockingFailureException} en vez de sobrescribir.
   *
   * <p><b>La transaccion es lo que hace el bloque atomico.</b> Sin ella, si el quinto
   * {@code save} fallaba los cuatro anteriores ya estaban escritos y la donacion quedaba
   * registrada a medias: los items de unos bienes y no los de otros.
   */
public void procesarPeticion(EntregaDTO request) {
    if (request == null) return;

    if (request.getDonacionResumen() == null) {
      throw new IllegalArgumentException("La peticion no trae el resumen de la donacion");
    }
    if (request.getEntidadBeneficiaria() == null) {
      throw new IllegalArgumentException("La peticion no trae la entidad beneficiaria");
    }

    List<BienDTO> bienes = request.getDonacionResumen().getBienes();
    List<UUID> idsDonaciones = request.getDonacionResumen().getIdsDonaciones();

    if (bienes == null || idsDonaciones == null) return;

    if (bienes.size() != idsDonaciones.size()) {
      throw new IllegalArgumentException("La cantidad de bienes (" + bienes.size()
              + ") no coincide con la cantidad de donaciones (" + idsDonaciones.size() + ")");
    }

    // El catalogo y la entidad van en SU PROPIA transaccion, y no junto con los items.
    //
    // Esto se midio con dos instancias: las dos recibieron una donacion para la misma entidad
    // beneficiaria, las dos insertaron la entidad, y una gano mientras la otra recibia la
    // violacion de clave primaria. Con todo en una sola transaccion, esa violacion marcaba el
    // rollback de TODO, y los items que la instancia perdedora estaba registrando se perdian: de
    // diez mensajes solo quedo uno en la base, y el listener se comio la excepcion creyendo que
    // era la carrera benigna del punto 21.
    //
    // Que dos instancias escriban el catalogo a la vez es normal y no puede arrastrar al trabajo
    // real, asi que va aparte.
    Entidad entidadDestino = resolverEntidad(request.getEntidadBeneficiaria());

    // Los items si van en una transaccion: o se registran todos los bienes del mensaje, o
    // ninguno. Registrar la mitad dejaria donaciones partidas.
    int[] conteo = itemsEnUnaTransaccion(bienes, idsDonaciones, entidadDestino);

    log.info("Peticion procesada: {} items registrados, {} repetidos omitidos", conteo[0],
            conteo[1]);
  }

  /**
   * Resuelve la entidad beneficiaria en su propia transaccion, tolerando la carrera entre
   * instancias.
   *
   * <p><b>Solo la entidad puede colisionar.</b> {@code Pais}, {@code Provincia},
   * {@code Ciudad} y {@code Direccion} generan su id, así que dos instancias pueden insertar filas
   * distintas sin problema (que es el punto 15, otra cosa). La que tiene clave natural es
   * {@code Entidad}, con {@code idEntidad} que viene del mensaje, y ahí sí las dos instancias
   * pueden pelear por el mismo INSERT.
   *
   * <p>Se busca antes de insertar, y si aun así choca, se vuelve a leer la que quedó. En los dos
   * caminos el resultado es el mismo, que es lo único que importa: que exista la entidad para
   * poder guardar el item.
   */
  private Entidad resolverEntidad(DireccionDTO dto) {
    Direccion direccion = this.convertirDireccionDTO(dto);
    if (direccion == null) {
      throw new IllegalArgumentException("La entidad beneficiaria no trae direccion");
    }

    return enSuPropiaTransaccion(() -> {
      repoPaises.save(direccion.getCiudad().getProvincia().getPais());
      repoProvincias.save(direccion.getCiudad().getProvincia());
      repoCiudades.save(direccion.getCiudad());
      repoDirecciones.save(direccion);

      UUID idEntidad = dto.getIdEntidad();

      Optional<Entidad> yaExistente = repoEntidades.findById(idEntidad);
      if (yaExistente.isPresent()) {
        return yaExistente.get();
      }

      try {
        return repoEntidades.saveAndFlush(new Entidad(idEntidad, direccion));
      } catch (DataIntegrityViolationException carrera) {
        // Otra instancia la inserto entre el findById y el save. Se relee y se sigue con la
        // que quedo: el resultado es exactamente el que se buscaba.
        log.info("La entidad {} ya fue registrada por otra instancia, se usa la existente",
                idEntidad);
        return repoEntidades.findById(idEntidad).orElseThrow(
                () -> new IllegalStateException(
                        "La entidad " + idEntidad + " no se pudo resolver tras una carrera", carrera));
      }
    });
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

        // Guarda de idempotencia: si el item ya existe, el mensaje es una repeticion. Se omite y
        // sigue con el resto de los bienes. Un `continue` y no un `return` porque la donacion
        // puede traer bienes nuevos junto con otros ya registrados.
        if (repoItemEntrega.existsById(idDonacion)) {
          repetidos++;
          log.info("La donacion {} ya estaba registrada, el mensaje se repite y se omite",
                  idDonacion);
          continue;
        }

        BienDTO bien = bienes.get(j);

        // Mapeo mediante el switch delegado al servicio
        UnidadDeMedida unidadDominio = mapearUnidadDeMedida(bien.getUnidadDeMedida());
        repoUnidades.save(unidadDominio);

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
   * <p>Se usa {@code REQUIRES_NEW} y no el_transactional del metodo porque aqui hace falta que el
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
   * <p><b>Transaccional por una razon concreta.</b> Publica el evento de trazabilidad y despues
   * guarda el item: sin transaccion, si el {@code save} fallaba el evento ya habia salido del
   * servicio y donaciones-service se enteraba de una entrega que en la base no ocurrio.
   *
   * <p>Con N instancias, dos operadores pueden confirmar la misma entrega casi al mismo tiempo.
   * El {@code @Version} del {@code ItemEntrega} hace que el segundo reciba
   * {@code OptimisticLockingFailureException} en vez de sobrescribir al primero, y el mensaje de
   * error le dice al operador que la entrega ya fue confirmada por otra peticion.
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
        if(comprobarExistencia(request.getFotoUrl())) {
          throw new IllegalArgumentException("Se requiere una foto para confirmar la entrega exitosa.");
        }
        repoItemEntrega.saveAndFlush(gestorPublicacionEventos.publicarEntregaConfirmada(item, repoRutas.findByIdDonacion(item.getIdDonacion())
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la ruta correspondiente a la donación " + idDonacion)), request.getFotoUrl()));
        break;

      case "NO_RECIBIDA":
        if(comprobarExistencia(request.getJustificacion())) {
          throw new IllegalArgumentException("Se requiere justificar el motivo por el cual falló la entrega.");
        }
        repoItemEntrega.saveAndFlush(gestorPublicacionEventos.publicarEntregaFallida(item, repoRutas.findByIdDonacion(item.getIdDonacion())
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la ruta correspondiente a la donación " + idDonacion)), request.getJustificacion()));
        break;

      case "PENDIENTE":
        // Reingreso a depósito tras revisión de una entrega NO_RECIBIDA.
        // reingresarADeposito() ya valida que solo se pueda hacer desde NO_RECIBIDA.
        repoItemEntrega.saveAndFlush(gestorPublicacionEventos.publicarReingresoDeposito(item));
        break;

      default:
        throw new IllegalArgumentException("Estado no válido. Use ENTREGADA, NO_RECIBIDA o PENDIENTE.");
    }

    repoItemEntrega.saveAndFlush(item);
  }

  /**
   * Ítems en estado NO_RECIBIDA, pendientes de revisión (reingreso a depósito
   * o replanificación). El control de quién puede llamar a este endpoint
   * es responsabilidad del front/capa de autorización, no de este servicio.
   */

  private boolean comprobarExistencia(String elemento){
    return (elemento == null || elemento.trim().isEmpty());
  }

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
