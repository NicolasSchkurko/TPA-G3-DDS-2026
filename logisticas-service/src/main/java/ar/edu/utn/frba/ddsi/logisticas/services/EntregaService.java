package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.*;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Ciudad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Pais;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Provincia;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

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
  public Page<BienDTO> findAll(Pageable pageable) {
      return repoItemEntrega.findAll(pageable).map(item -> new BienDTO().aplicar(item));
  }

  public BienDTO findById(UUID id) {
    Optional<ItemEntrega> item = repoItemEntrega.findById(id);
    if (item.isEmpty()){
      throw new RecursoNoEncontradoException("Entrega no encontrada");
    }
    return new BienDTO().aplicar(item.get());
  }

  public void delete(UUID id) {
    Optional<ItemEntrega> item = repoItemEntrega.findById(id);
    if(item.isEmpty()){
      throw new RecursoNoEncontradoException("Entrega no encontrada");
    }
    repoItemEntrega.deleteById(id);
  }

  // --- MÉTODOS DE NEGOCIO ---

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
    direccion.setLatitud(dto.getLatitud());
    direccion.setLongitud(dto.getLongitud());

    return repoDirecciones.save(direccion);
  }

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

  private <T> T enSuPropiaTransaccion(java.util.function.Supplier<T> bloque) {
    org.springframework.transaction.support.TransactionTemplate plantilla =
            new org.springframework.transaction.support.TransactionTemplate(gestorTransacciones);
    plantilla.setPropagationBehavior(
            org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return plantilla.execute(estado -> bloque.get());
  }

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

  @Transactional
  public void actualizarEstado(UUID idDonacion, ActualizacionEntregaDTO request) {
    ItemEntrega item = repoItemEntrega.findById(idDonacion)
            .orElseThrow(() -> new RecursoNoEncontradoException("Donación no encontrada con el ID proporcionado"));

    if (request.getEstado() == null) {
      throw new IllegalArgumentException("El estado no puede ser nulo");
    }

    switch (request.getEstado().toUpperCase()) {
      case "ENTREGADA":
        if(faltaTexto(request.getFotoUrl())) {
          throw new IllegalArgumentException("Se requiere una foto para confirmar la entrega exitosa.");
        }
        Ruta rutaEntregada = repoRutas.findFirstByParadasItemsIdDonacion(item.getIdDonacion())
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la ruta correspondiente a la donación " + idDonacion));
        repoItemEntrega.save(gestorPublicacionEventos.publicarEntregaConfirmada(item, rutaEntregada, request.getFotoUrl()));
        sincronizarItemEnRuta(rutaEntregada, item);
        break;

      case "NO_RECIBIDA":
        if(faltaTexto(request.getJustificacion())) {
          throw new IllegalArgumentException("Se requiere justificar el motivo por el cual falló la entrega.");
        }
        Ruta rutaFallida = repoRutas.findFirstByParadasItemsIdDonacion(item.getIdDonacion())
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la ruta correspondiente a la donación " + idDonacion));
        repoItemEntrega.save(gestorPublicacionEventos.publicarEntregaFallida(item, rutaFallida, request.getJustificacion()));
        sincronizarItemEnRuta(rutaFallida, item);
        break;

      case "PENDIENTE":
        if (item.getEstado() != EstadoEntrega.NO_RECIBIDA) {
          throw new IllegalStateException("Solo se puede reingresar a depósito una entrega en estado NO_RECIBIDA (estado actual: "
                  + item.getEstado() + ").");
        }
        repoItemEntrega.save(gestorPublicacionEventos.publicarReingresoDeposito(item));
        repoRutas.findFirstByParadasItemsIdDonacion(item.getIdDonacion()).ifPresent(ruta -> sincronizarItemEnRuta(ruta, item));
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

  public BienesDTO obtenerEntregasNoRecibidas() {
    List<ItemEntrega> items = repoItemEntrega.findByEstado(EstadoEntrega.NO_RECIBIDA);
    return new BienesDTO().aplicar(items);
  }
}
