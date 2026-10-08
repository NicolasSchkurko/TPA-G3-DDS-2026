package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.BienResumenDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.entrega.BienDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.entrega.EntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.messaging.ProductorLogistica;
import ar.edu.utn.frba.ddsi.donaciones.dto.ResultadoMatchmakingDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.FormularioRequestDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AsignadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.*;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DonacionService {
  private final GestorAsignaciones gestorAsignaciones;
  private final GestorFormulario gestorFormulario;
  private final GestorMatchmaking gestorMatchmaking;
  private final RepositorioSubcategoriasDeBienes repositorioSubcategoriasDeBienes;
  private final RepositorioDonaciones repositorioDonaciones;
  private final RepositorioDonantes repositorioDonantes;
  private final RepositorioFormularios repositorioFormularios;
  private final RepositorioEntidadesBeneficiarias repositorioEntidadesBeneficiarias;
  private final RepositorioDeResultadosMatchmaking repositorioDeResultadosMatchmaking;
  private final RepositorioBienes repositorioBienes;
  private final ProductorLogistica productorLogistica;

  public DonacionService(GestorAsignaciones gestorAsignaciones,
                         GestorFormulario gestorFormulario, GestorMatchmaking gestorMatchmaking,
                         RepositorioSubcategoriasDeBienes repositorioSubcategoriasDeBienes,
                         RepositorioDonaciones repositorioDonaciones,
                         RepositorioDonantes repositorioDonantes,
                         RepositorioFormularios repositorioFormularios,
                         RepositorioEntidadesBeneficiarias repositorioEntidadesBeneficiarias,
                         RepositorioDeResultadosMatchmaking repositorioDeResultadosMatchmaking,
                         RepositorioBienes repositorioBienes,
                        ProductorLogistica productorLogistica) {
    this.gestorAsignaciones = gestorAsignaciones;
    this.gestorFormulario = gestorFormulario;
    this.gestorMatchmaking = gestorMatchmaking;
    this.repositorioSubcategoriasDeBienes = repositorioSubcategoriasDeBienes;
    this.repositorioDonaciones = repositorioDonaciones;
    this.repositorioDonantes = repositorioDonantes;
    this.repositorioFormularios = repositorioFormularios;
    this.repositorioEntidadesBeneficiarias = repositorioEntidadesBeneficiarias;
    this.repositorioDeResultadosMatchmaking = repositorioDeResultadosMatchmaking;
    this.repositorioBienes = repositorioBienes;
    this.productorLogistica = productorLogistica;
  }

  public List<DonacionDTO> obtenerTodas() {
    return repositorioDonaciones.obtenerTodos().stream()
                           .map(DonacionDTO::from).collect(Collectors.toList());
  }

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

  @Transactional(rollbackFor = Exception.class)
  public void ejecutarMatchmakingADemanda() {
    AsignadorDonaciones asignadorDonaciones = new AsignadorDonaciones(gestorMatchmaking,gestorAsignaciones,repositorioDeResultadosMatchmaking);
    List<Donacion> donacionesNoAsignadas = repositorioDonaciones.buscarDonacionesSinAsignar();
    List<EntidadBeneficiaria> entidades = repositorioEntidadesBeneficiarias.obtenerTodas();
    asignadorDonaciones.ejecutarMatchmakingBatch(donacionesNoAsignadas,entidades);
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
      // Los bienes que la lista deja afuera no se borran (ver RepositorioBienes: la fila es
      // compartida con Formulario.donaciones) ni se cuentan como entregados, así que reescribir
      // el contenido de una donación ya asignada/entregada dejaría los conteos de
      // Necesidad.cantidadRecibida() sin correspondencia con lo realmente entregado. Por eso el
      // PUT sólo se permite mientras la donación sigue en depósito.
      if (existente.getEstado() != Estado.EN_DEPOSITO) {
        throw new IllegalArgumentException(
                "No se pueden modificar los bienes de una donación que no está en depósito (estado actual: " + existente.getEstado() + ")");
      }
      List<Bien> bienesActualizados = resolverBienes(dto.getBienes());
      bienesActualizados.forEach(this::crearBien);
      existente.setBienes(bienesActualizados);
    }

    return DonacionDTO.from(repositorioDonaciones.actualizar(existente.getId(), existente).get());
  }

  @Transactional(rollbackFor = Exception.class)
  public void eliminarDonacion(UUID id) {
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

  public List<ResultadoMatchmakingDTO> obtenerTodosLosResultadosMatchmaking() {
    return repositorioDeResultadosMatchmaking.findAll().stream()
                            .map(ResultadoMatchmakingDTO::from).collect(Collectors.toList());
  }

  @Transactional(rollbackFor = Exception.class)
  public void asignarPropuesta(UUID donacionId, Integer posicion) {
    PropuestaAsignacion propuestaAsignacion = gestorMatchmaking.obtenerPropuestaSeleccionadaParaDonacion(donacionId, posicion);
    Donacion donacion = repositorioDonaciones.obtenerPorId(donacionId).orElseThrow(() -> new IllegalArgumentException("No se encontró la donación"));
    gestorAsignaciones.asignarPropuesta(donacion, propuestaAsignacion);
    eliminarResultadoMatchmaking(donacion.getId());
    gestorAsignaciones.cambiarEstado(donacion.getId(), "ASIGNADO", "Donacion Asignada");

    // A partir de aca la donacion le corresponde a logistica. El enunciado pide que esta
    // integracion vaya por broker, asi que se publica el item de entrega en vez de llamar a
    // logistica por HTTP. Va despues del cambio de estado a proposito: si la publicacion
    // falla, la excepcion sube y la transaccion se revierte, y no queda una donacion
    // marcada como asignada que nadie va a entregar nunca.
    publicarEntregaALogistica(donacionId);
  }

  /**
   * Arma el mensaje de entrega y lo publica en el exchange de integracion.
   *
   * <p>El mensaje lleva lo minimo que logistica necesita para crear el item: los ids de las
   * donaciones, los bienes con su cantidad y unidad, y la direccion de la entidad
   * beneficiaria. Logistica no consulta este servicio para nada mas, que es lo que exige el
   * enunciado: no debe invocar los servicios de donaciones ni incentivos, sino dejar
   * disponible la informacion.
   */
  private void publicarEntregaALogistica(UUID donacionId) {
    Donacion donacion = repositorioDonaciones.obtenerPorId(donacionId)
            .orElseThrow(() -> new IllegalArgumentException("No se encontro la donacion"));

    EntidadBeneficiaria entidad = donacion.getEntidad();
    if (entidad == null || entidad.getDireccion() == null) {
      // Sin entidad asignada todavia no hay donde entregar. No es un error: el matchmaking
      // puede haber asignado solo el estado y la entidad viene en un paso posterior.
      return;
    }

    // Bien guarda la cantidad en el campo 'peso' y la unidad en 'unidadUtilizada'; el DTO de
    // transporte los llama cantidad y unidadDeMedida, asi que se renombran al mapear.
    List<BienDTO> bienes = donacion.getBienes().stream()
            .map(b -> new BienDTO(b.getPeso(), b.getUnidadUtilizada().name()))
            .collect(Collectors.toList());

    EntregaDTO entrega = new EntregaDTO(
            List.of(donacion.getId()),
            bienes,
            DireccionDTO.from(entidad.getDireccion())
    );

    productorLogistica.publicarDonacionAsignada(entrega);
  }

  // Resuelve (o crea) la SubcategoriaBien del catálogo compartido ANTES de construir el Bien,
  // mismo patrón que usamos para Necesidad, para no romper merge() ni duplicar el catálogo.
  private Bien resolverBien(BienResumenDTO dto) {
    SubcategoriaBien subcategoria = repositorioSubcategoriasDeBienes.obtenerOCrearSubcategoria(dto.getCategoria(), dto.getSubcategoria());
    return dto.toDomain(subcategoria);
  }

  // dto.toDomain() tira IllegalArgumentException en vez de devolver null (ver BienResumenDTO):
  // acá se agrega el índice del item para que el 400 resultante diga cuál de la lista está mal,
  // en vez de dejar que un bien sin tipoBien llegue como null a la segmentación (NPE/500).
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