package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.clients.NotificacionesClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.BienResumenDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.PropuestaAsignacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.ResultadoMatchmakingDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.FormularioRequestDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.*;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadRecurrente;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorBienes;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorDonantes;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorFormulario;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorMatchmaking;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DonacionService {
  private final GestorEntidadesBeneficiarias gestorEntidades;
  private final GestorDonantes gestorDonantes;
  private final GestorDonaciones gestorDonaciones;
  private final GestorFormulario gestorFormulario;
  private final GestorMatchmaking gestorMatchmaking;
  private final GestorBienes gestorBienes;
  private final NotificacionesClient notificacionesClient;

  public DonacionService(GestorEntidadesBeneficiarias gestorEntidades,
                         GestorDonantes gestorDonantes,
                         GestorDonaciones gestorDonaciones,
                         GestorFormulario gestorFormulario,
                         GestorMatchmaking gestorMatchmaking,
                         GestorBienes gestorBienes,
                         NotificacionesClient notificacionesClient) {
    this.gestorEntidades = gestorEntidades;
    this.gestorDonantes = gestorDonantes;
    this.gestorDonaciones = gestorDonaciones;
    this.gestorFormulario = gestorFormulario;
    this.gestorMatchmaking = gestorMatchmaking;
    this.gestorBienes = gestorBienes;
    this.notificacionesClient = notificacionesClient;
  }

  public List<DonacionDTO> obtenerTodas() {
    return gestorDonaciones.obtenerTodasLasDonaciones().stream()
                           .map(this::donacionToDTO)
                           .collect(Collectors.toList());
  }

  public DonacionDTO obtenerPorId(UUID id) {
    Donacion donacion = gestorDonaciones.obtenerDonacionPorId(id)
                                        .orElseThrow(() -> new IllegalArgumentException("No se encontro la donacion"));
    return donacionToDTO(donacion);
  }

  public List<DonacionDTO> procesarFormulario(FormularioRequestDTO request) {
    Donante donante = gestorDonantes.obtenerDonante(request.getIdDonante());
    if (donante == null) {
      throw new NullPointerException("No se encontró persona con ese ID");
    }

    List<Bien> bienesNormal = maptodto(request.getBienes());

    if (bienesNormal != null) {
      bienesNormal.forEach(gestorBienes::crearBien);
    }

    Formulario formularioGenerado = gestorFormulario.crearFormulario(donante, bienesNormal, request.getFechaRealizacion());
    List<Donacion> donacionesProcesadas = gestorFormulario.procesarFormulario(formularioGenerado);

    gestorDonaciones.guardarDonaciones(donacionesProcesadas);
    gestorDonantes.agregarFormularioADonante(donante.getId(), formularioGenerado);

    return donacionesProcesadas.stream().map(this::donacionToDTO).collect(Collectors.toList());
  }

  public void ejecutarMatchmakingADemanda() {
    List<Donacion> donacionesNoAsignadas = gestorDonaciones.listarPendientesDeAsignacion();
    List<EntidadBeneficiaria> entidades = gestorEntidades.listarTodasLasEntidades();

    List<ResultadoMatchmaking> resultados = gestorDonaciones.asignarDonaciones(donacionesNoAsignadas, entidades);
    gestorMatchmaking.guardarResultados(resultados);
  }

  public DonacionDTO actualizarDonacion(UUID id, DonacionDTO actualizacionDto) {
    Donacion donacionDominio = dtoToDonacion(actualizacionDto);
    Donacion actualizada = gestorDonaciones.actualizarDonacion(id, donacionDominio);
    return donacionToDTO(actualizada);
  }

  public void eliminarDonacion(UUID id) {
    gestorDonaciones.eliminarDonacion(id);
  }

  public DonacionDTO cambiarEstado(UUID id, String nuevoEstado, String justificacion) {
    Donacion donacion = gestorDonaciones.cambiarEstado(id, nuevoEstado, justificacion);
    return donacionToDTO(donacion);
  }

  public DonacionDTO marcarComoVencida(UUID id) {
    Donacion donacion = gestorDonaciones.cambiarEstado(id, "VENCIDA", "Registrado como vencido por la administración.");
    return donacionToDTO(donacion);
  }

  public List<ResultadoMatchmakingDTO> obtenerTodosLosResultadosMatchmaking() {
    return gestorMatchmaking.obtenerTodosLosResultadosMatchmaking().stream()
                            .map(this::resultadoToDTO)
                            .collect(Collectors.toList());
  }

  public void asignarPropuesta(UUID donacionId, Integer posicion) {
    Donacion donacion = gestorMatchmaking.asignarPropuesta(donacionId, posicion);
    gestorDonaciones.actualizarDonacion(donacionId, donacion);
    gestorEntidades.modificarEntidad(donacion.getEntidad().getId(), donacion.getEntidad());
    notificarAsignacion(donacion);
  }

  private void notificarAsignacion(Donacion donacion) {
    try {
      if (donacion.getEntidad() != null && donacion.getEntidad().getPersonaJuridica() != null) {
        NotificacionDTO notificacionEntidad = new NotificacionDTO(
            donacion.getEntidad().getPersonaJuridica().getMediosDeContacto().getMedioDeContactoPredeterminado().getTipo(),
            donacion.getEntidad().getPersonaJuridica().getMediosDeContacto().getMedioDeContactoPredeterminado().getValor(),
            "Se le ha asignado una nueva donación de la categoría: " + donacion.getSubcategoria().getNombre(),
            "Nueva Donación Asignada"
        );
        notificacionesClient.enviarNotificacion(notificacionEntidad);
      }

      if (donacion.getDonante() != null && donacion.getDonante().getPersona() != null) {
        String razonSocialEntidad = donacion.getEntidad() != null && donacion.getEntidad().getPersonaJuridica() != null
                                    ? donacion.getEntidad().getPersonaJuridica().getRazonSocial() : "una Entidad Beneficiaria";

        NotificacionDTO notificacionDonante = new NotificacionDTO(
            donacion.getDonante().getPersona().getMediosDeContacto().getMedioDeContactoPredeterminado().getTipo(),
            donacion.getDonante().getPersona().getMediosDeContacto().getMedioDeContactoPredeterminado().getValor(),
            "Su donación ha sido asignada a " + razonSocialEntidad,
            "Donación Asignada a Entidad"
        );
        notificacionesClient.enviarNotificacion(notificacionDonante);
      }
    } catch (Exception e) {
      System.err.println("Error al enviar notificaciones asíncronas: " + e.getMessage());
    }
  }

  // --- MAPPERS INTERNOS DE DONACION Y MATCHMAKING ---

  private DonacionDTO donacionToDTO(Donacion donacion) {
    if (donacion == null) return null;
    DonacionDTO dto = new DonacionDTO();

    dto.setDonanteName(donacion.getDonante() != null && donacion.getDonante().getPersona() != null
                       ? donacion.getDonante().getPersona().getNombreDeUsuario() : "Desconocido");

    dto.setEntidadBeneficiaria(donacion.getEntidad() != null && donacion.getEntidad().getPersonaJuridica() != null
                               ? donacion.getEntidad().getPersonaJuridica().getRazonSocial() : "No asignada");

    dto.setDescripcion(donacion.getDescripcion());
    dto.setEstado(donacion.getEstado() != null ? donacion.getEstado().name() : "N/A");
    dto.setSubcategoriaName(donacion.getSubcategoria() != null ? donacion.getSubcategoria().getNombre() : "N/A");
    dto.setCategoriaBienName(donacion.getSubcategoria() != null && donacion.getSubcategoria().getCategoria() != null
                             ? donacion.getSubcategoria().getCategoria().getNombre() : "N/A");

    dto.setFechaEntrega(donacion.getFechaEntrega());
    dto.setCantidadTotalBienes(donacion.sumaCantidadBienes());
    dto.setBienes(new ArrayList<>());
    return dto;
  }

  private Donacion dtoToDonacion(DonacionDTO donacionDTO) {
    if (donacionDTO == null) return null;
    Donacion donacion = new Donacion();
    donacion.setDescripcion(donacionDTO.getDescripcion());
    if (donacionDTO.getBienes() != null) {
      donacion.setBienes(maptodto(donacionDTO.getBienes()));
    }
    return donacion;
  }

  private List<Bien> maptodto(List<BienResumenDTO> bienes) {
    if (bienes == null) return new ArrayList<>();

    List<Bien> b = new ArrayList<>();
    for (BienResumenDTO x : bienes) {
      if (x == null || x.getTipoBien() == null) continue;

      String nombreCat = x.getCategoria() != null && !x.getCategoria().trim().isEmpty() ? x.getCategoria() : "General";
      String nombreSub = x.getSubcategoria() != null && !x.getSubcategoria().trim().isEmpty() ? x.getSubcategoria() : "General";
      CategoriaBien categoria = new CategoriaBien(nombreCat);
      SubcategoriaBien subcategoria = new SubcategoriaBien(nombreSub, categoria);

      Bien bienNormal;
      switch (x.getTipoBien().toUpperCase()) {
        case "CON_ESTADO":
        case "CONESTADO":
          bienNormal = new BienConEstado(
              x.getDescripcion(),
              subcategoria,
              x.getUrlFoto(),
              x.getCantidad() != null ? x.getCantidad() : 0,
              mapToUM(x.getUnidadDeMedida()),
              x.getUsado() != null ? x.getUsado() : false
          );
          b.add(bienNormal);
          break;
        case "PERECEDERO":
          bienNormal = new BienPerecedero(
              x.getDescripcion(),
              subcategoria,
              x.getUrlFoto(),
              x.getCantidad() != null ? x.getCantidad() : 0,
              mapToUM(x.getUnidadDeMedida()),
              x.getFechaVencimiento() != null ? x.getFechaVencimiento() : LocalDate.now().plusMonths(1)
          );
          b.add(bienNormal);
          break;
        default:
          System.err.println("Tipo de bien desconocido: " + x.getTipoBien());
          break;
      }
    }
    return b;
  }

  private UnidadDeMedida mapToUM(String unidad) {
    // ARREGLADO: Ya no utiliza UNIDADES que no existe en el enum.
    // Devuelve null si la unidad no es explícitamente enviada.
    if (unidad == null) return null;
    return switch (unidad.toUpperCase()) {
      case "KILOGRAMOS", "KILOS", "KG" -> UnidadDeMedida.KILOGRAMOS;
      case "LITROS", "LT" -> UnidadDeMedida.LITROS;
      default -> null;
    };
  }

  private ResultadoMatchmakingDTO resultadoToDTO(ResultadoMatchmaking resultado) {
    if (resultado == null) return null;
    ResultadoMatchmakingDTO dto = new ResultadoMatchmakingDTO();
    dto.setDonacion(donacionToDTO(resultado.getDonacion()));

    if (resultado.getPropuestasOrdenadas() != null) {
      dto.setPropuestasOrdenadas(
          resultado.getPropuestasOrdenadas().stream().map(this::propuestaToDTO).toList()
      );
    }
    dto.setHuboCoincidenciaTotal(resultado.isHuboCoincidenciaTotal());
    return dto;
  }

  private PropuestaAsignacionDTO propuestaToDTO(PropuestaAsignacion propuesta) {
    if (propuesta == null) return null;
    PropuestaAsignacionDTO dto = new PropuestaAsignacionDTO();

    if (propuesta.getEntidad() != null) {
      EntidadBeneficiariaDTO entidadDTO = new EntidadBeneficiariaDTO();
      if (propuesta.getEntidad().getPersonaJuridica() != null) {
        entidadDTO.setRazonSocial(propuesta.getEntidad().getPersonaJuridica().getRazonSocial());
        if (propuesta.getEntidad().getPersonaJuridica().getMediosDeContacto() != null &&
            propuesta.getEntidad().getPersonaJuridica().getMediosDeContacto().getMedioDeContactoPredeterminado() != null) {
          entidadDTO.setTelefono(propuesta.getEntidad().getPersonaJuridica().getMediosDeContacto().getMedioDeContactoPredeterminado().getValor());
        }
      }
      dto.setEntidad(entidadDTO);
    }

    if (propuesta.getNecesidad() != null) {
      Necesidad necesidad = propuesta.getNecesidad();
      NecesidadDTO necesidadDTO = new NecesidadDTO();
      necesidadDTO.setDescripcion(necesidad.getDescripcion());
      necesidadDTO.setCantidadObjetivo(necesidad.getCantidadObjetivo());
      if (necesidad.getSubcategoria() != null) {
        necesidadDTO.setNombreSubcategoria(necesidad.getSubcategoria().getNombre());
        if (necesidad.getSubcategoria().getCategoria() != null) {
          necesidadDTO.setNombreCategoria(necesidad.getSubcategoria().getCategoria().getNombre());
        }
      }
      necesidadDTO.setTipoNecesidad(necesidad instanceof NecesidadRecurrente ? "RECURRENTE" : "EXTRAORDINARIA");
      if (necesidad instanceof NecesidadRecurrente recurrente) {
        necesidadDTO.setPlazoEnDias(recurrente.getPlazoEnDias());
      }
      dto.setNecesidad(necesidadDTO);
    }

    dto.setAlgoritmo(propuesta.getAlgoritmo());
    dto.setPosicion(propuesta.getPosicion());
    dto.setScore(propuesta.getScore());
    return dto;
  }
}