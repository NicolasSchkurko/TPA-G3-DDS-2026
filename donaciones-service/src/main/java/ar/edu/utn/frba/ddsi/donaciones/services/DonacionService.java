package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.clients.NotificacionesClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.BienResumenDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.ResultadoMatchmakingDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.FormularioRequestDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AsignadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.*;
import org.springframework.stereotype.Service;

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
  private final GestorNecesidades gestorNecesidades;

  public DonacionService(GestorEntidadesBeneficiarias gestorEntidades, GestorDonantes gestorDonantes,
                         GestorDonaciones gestorDonaciones, GestorFormulario gestorFormulario,
                         GestorMatchmaking gestorMatchmaking, GestorBienes gestorBienes,
                         NotificacionesClient notificacionesClient, GestorNecesidades gestorNecesidades) {
    this.gestorEntidades = gestorEntidades;
    this.gestorDonantes = gestorDonantes;
    this.gestorDonaciones = gestorDonaciones;
    this.gestorFormulario = gestorFormulario;
    this.gestorMatchmaking = gestorMatchmaking;
    this.gestorBienes = gestorBienes;
    this.notificacionesClient = notificacionesClient;
    this.gestorNecesidades=gestorNecesidades;
  }

  public List<DonacionDTO> obtenerTodas() {
    return gestorDonaciones.obtenerTodasLasDonaciones().stream()
                           .map(DonacionDTO::from).collect(Collectors.toList());
  }

  public DonacionDTO obtenerPorId(UUID id) {
    return DonacionDTO.from(gestorDonaciones.obtenerDonacionPorId(id).orElseThrow(() -> new IllegalArgumentException("No se encontró la donación")));
  }

  public List<DonacionDTO> procesarFormulario(FormularioRequestDTO request) {
    Donante donante = gestorDonantes.obtenerDonante(request.getIdDonante());
    if (donante == null) throw new NullPointerException("No se encontró persona con ese ID");

    List<Bien> bienesNormal = request.getBienes() != null ? request.getBienes().stream().map(BienResumenDTO::toDomain).collect(Collectors.toList()) : List.of();
    bienesNormal.forEach(gestorBienes::crearBien);

    Formulario formularioGenerado = gestorFormulario.crearFormulario(donante, bienesNormal, request.getFechaRealizacion());
    List<Donacion> donacionesProcesadas = gestorFormulario.procesarFormulario(formularioGenerado);

    gestorDonaciones.guardarDonaciones(donacionesProcesadas);
    gestorDonantes.agregarFormularioADonante(donante.getId(), formularioGenerado);

    return donacionesProcesadas.stream().map(DonacionDTO::from).collect(Collectors.toList());
  }

  public void ejecutarMatchmakingADemanda() {
    AsignadorDonaciones asignadorDonaciones = new AsignadorDonaciones(gestorMatchmaking,gestorDonaciones);
    List<Donacion> donacionesNoAsignadas = gestorDonaciones.listarSinAsignacion();
    List<EntidadBeneficiaria> entidades = gestorEntidades.listarTodasLasEntidades();
    asignadorDonaciones.ejecutarMatchmakingBatch(donacionesNoAsignadas,entidades);
  }

  public DonacionDTO actualizarDonacion(UUID id, DonacionDTO dto) {
    return DonacionDTO.from(gestorDonaciones.actualizarDonacion(id, dto.toDomain()));
  }

  public void eliminarDonacion(UUID id) {
    gestorDonaciones.eliminarDonacion(id);
  }

  public DonacionDTO cambiarEstado(UUID id, String nuevoEstado, String justificacion) {
    return DonacionDTO.from(gestorDonaciones.cambiarEstado(id, nuevoEstado, justificacion));
  }

  public DonacionDTO marcarComoVencida(UUID id) {
    return DonacionDTO.from(gestorDonaciones.cambiarEstado(id, "VENCIDA", "Registrado como vencido por la administración."));
  }

  public List<ResultadoMatchmakingDTO> obtenerTodosLosResultadosMatchmaking() {
    return gestorMatchmaking.obtenerTodosLosResultadosMatchmaking().stream()
                            .map(ResultadoMatchmakingDTO::from).collect(Collectors.toList());
  }

  public void asignarPropuesta(UUID donacionId, Integer posicion) {
    PropuestaAsignacion propuestaAsignacion = gestorMatchmaking.obtenerPropuestaSeleccionadaParaDonacion(donacionId, posicion);
    Donacion donacion = gestorDonaciones.obtenerDonacionPorId(donacionId).orElseThrow(() -> new IllegalArgumentException("No se encontró la donación"));
    gestorDonaciones.asignarEntidad(donacion.getId(), propuestaAsignacion.getEntidad());
    gestorDonaciones.cambiarEstado(donacion.getId(), "ASIGNADO", "Donacion Asignada");
    gestorNecesidades.agregarDonacionANecesidad(propuestaAsignacion.getNecesidad().getId(), donacion);
    gestorMatchmaking.eliminarResultado(donacionId);
    notificarAsignacion(donacion);
  }

  private void notificarAsignacion(Donacion donacion) {
    try {
      if (donacion.getEntidad() != null && donacion.getEntidad().getPersonaJuridica() != null) {
        NotificacionDTO notifEntidad = new NotificacionDTO(
            donacion.getEntidad().getPersonaJuridica().getMediosDeContacto().getMedioDeContactoPredeterminado().getTipo(),
            donacion.getEntidad().getPersonaJuridica().getMediosDeContacto().getMedioDeContactoPredeterminado().getValor(),
            "Se le ha asignado una nueva donación de la categoría: " + donacion.getSubcategoria().getNombre(), "Nueva Donación Asignada"
        );
        notificacionesClient.enviarNotificacion(notifEntidad);
      }
      if (donacion.getDonante() != null && donacion.getDonante().getPersona() != null) {
        String rsEntidad = (donacion.getEntidad() != null && donacion.getEntidad().getPersonaJuridica() != null) ? donacion.getEntidad().getPersonaJuridica().getRazonSocial() : "una Entidad Beneficiaria";
        NotificacionDTO notifDonante = new NotificacionDTO(
            donacion.getDonante().getPersona().getMediosDeContacto().getMedioDeContactoPredeterminado().getTipo(),
            donacion.getDonante().getPersona().getMediosDeContacto().getMedioDeContactoPredeterminado().getValor(),
            "Su donación ha sido asignada a " + rsEntidad, "Donación Asignada a Entidad"
        );
        notificacionesClient.enviarNotificacion(notifDonante);
      }
    } catch (Exception e) { System.err.println("Error al enviar notificaciones asíncronas: " + e.getMessage()); }
  }
}