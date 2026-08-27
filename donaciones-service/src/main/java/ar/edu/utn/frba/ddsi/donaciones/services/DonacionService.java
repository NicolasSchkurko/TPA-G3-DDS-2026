package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.*;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorDonantes;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorFormulario;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorMatchmaking;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DonacionService {
  private final GestorEntidadesBeneficiarias gestorEntidades;
  private final GestorDonantes gestorDonantes;
  private final GestorDonaciones gestorDonaciones;
  private final GestorFormulario gestorFormulario;
  private final GestorMatchmaking gestorMatchmaking;

  public DonacionService(GestorEntidadesBeneficiarias gestorEntidades,
                         GestorDonantes gestorDonantes,
                         GestorDonaciones gestorDonaciones,
                         GestorFormulario gestorFormulario,
                         GestorMatchmaking gestorMatchmaking) {
    this.gestorEntidades = gestorEntidades;
    this.gestorDonantes = gestorDonantes;
    this.gestorDonaciones = gestorDonaciones;
    this.gestorFormulario = gestorFormulario;
      this.gestorMatchmaking = gestorMatchmaking;
  }

  public List<Donacion> obtenerTodas() {
    return gestorDonaciones.obtenerTodasLasDonaciones();
  }

  public Optional<Donacion> obtenerPorId(UUID id) {
    return gestorDonaciones.obtenerDonacionPorId(id);
  }

  public List<Donacion> procesarFormulario(UUID idDonante, List<Bien> bienesNormal, LocalDate fechaRealizacion) {

    Donante donante = gestorDonantes.obtenerDonante(idDonante);
    if (donante == null) {
      throw new NullPointerException("No se encontró persona con ese ID");
    }

    List<Donacion> donacionesProcesadas = gestorFormulario.procesarFormulario(donante, bienesNormal, fechaRealizacion);
    gestorDonaciones.guardarDonaciones(donacionesProcesadas);
    //agregarFormularioADonante de gestordonante

    return donacionesProcesadas;
  }

  public void asignarDonaciones() {
    List<Donacion> donacionesNoAsignadas = gestorDonaciones.listarPendientesDeAsignacion();
    List<EntidadBeneficiaria> entidades = gestorEntidades.listarTodasLasEntidades();

    List<ResultadoMatchmaking> resultados = gestorDonaciones.asignarDonaciones(donacionesNoAsignadas, entidades);
    gestorMatchmaking.guardarResultados(resultados);
  }

  public Donacion actualizarDonacion(UUID id, Donacion actualizacion) {
    return gestorDonaciones.actualizarDonacion(id, actualizacion);
  }

  public void eliminarDonacion(UUID id) {
    gestorDonaciones.eliminarDonacion(id);
  }

  public Donacion cambiarEstado(UUID id, String nuevoEstado, String justificacion) {
    return gestorDonaciones.cambiarEstado(id, nuevoEstado, justificacion);
  }

  public List<ResultadoMatchmaking> obtenerTodosLosResultadosMatchmaking() {
    return gestorMatchmaking.obtenerTodosLosResultadosMatchmaking();
  }

  public void asignarPropuesta(UUID donacionId, Integer posicion) {
    //Buscar resultado y asignar
    Donacion donacion = gestorMatchmaking.asignarPropuesta(donacionId, posicion);

    gestorDonaciones.actualizarDonacion(donacionId, donacion);
    // Uso del Gestor de Entidades para actualizar la entidad si es necesario
    gestorEntidades.modificarEntidad(donacion.getEntidad().getId(), donacion.getEntidad());
  }

}