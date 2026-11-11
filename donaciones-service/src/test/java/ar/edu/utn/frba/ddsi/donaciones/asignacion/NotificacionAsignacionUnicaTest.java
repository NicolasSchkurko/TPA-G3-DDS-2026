package ar.edu.utn.frba.ddsi.donaciones.asignacion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.donaciones.clients.NotificacionesClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.TipoJuridico;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiaNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiasMensajes.NotificacionDonacionAsignada;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.ServicioNotificaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorAsignaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeNecesidades;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Punto 53: la asignación notifica una sola vez por destinatario")
class NotificacionAsignacionUnicaTest {

  @Test
  @DisplayName("asignarPropuesta + cambiarEstado(ASIGNADO) produce un único aviso por destinatario")
  void notificaUnaSolaVezPorDestinatario() throws Exception {
    NotificacionesClient client = mock(NotificacionesClient.class);
    FabricaEstrategiasNotificacion fabrica = fabricaConEstrategiaReal(client);
    RepositorioDeDonaciones repoDonaciones = mock(RepositorioDeDonaciones.class);
    RepositorioDeNecesidades repoNecesidades = mock(RepositorioDeNecesidades.class);

    Donacion donacion = donacionAsignable();
    when(repoDonaciones.obtenerPorId(donacion.getId())).thenReturn(Optional.of(donacion));

    Necesidad necesidad = mock(Necesidad.class);
    when(necesidad.getId()).thenReturn(UUID.randomUUID());
    PropuestaAsignacion propuesta = mock(PropuestaAsignacion.class);
    when(propuesta.getEntidad()).thenReturn(donacion.getEntidad());
    when(propuesta.getNecesidad()).thenReturn(necesidad);

    GestorAsignaciones gestor = crearGestor(client, repoDonaciones, repoNecesidades, fabrica);

    gestor.asignarPropuesta(donacion, propuesta);
    gestor.cambiarEstado(donacion.getId(), "ASIGNADO", "test");

    // Estrategia: un aviso a la entidad y otro al donante. El camino directo anterior sumaba
    // dos más (los predeterminados) y este verify fallaba con 4.
    verify(client, times(2)).enviarNotificacion(any(NotificacionDTO.class));
  }

  private FabricaEstrategiasNotificacion fabricaConEstrategiaReal(NotificacionesClient client) {
    List<EstrategiaNotificacion> estrategias = new ArrayList<>();
    for (TipoEventoNotificacion tipo : TipoEventoNotificacion.values()) {
      if (tipo == TipoEventoNotificacion.DONACION_ASIGNADA) {
        continue;
      }
      EstrategiaNotificacion otra = mock(EstrategiaNotificacion.class);
      when(otra.getTipoEvento()).thenReturn(tipo);
      estrategias.add(otra);
    }
    estrategias.add(new NotificacionDonacionAsignada(new ServicioNotificaciones(client)));
    return new FabricaEstrategiasNotificacion(estrategias);
  }

  private Donacion donacionAsignable() {
    CategoriaBien categoria = new CategoriaBien("Alimentos");
    SubcategoriaBien subcategoria = new SubcategoriaBien("Leche", categoria);

    Mail mailEntidad = new Mail("entidad@test.com");
    Juridica juridica = new Juridica("Comedor", "Alimentos", TipoJuridico.ONG,
        "30-11111111-9", new ArrayList<>());
    juridica.getMediosDeContacto().agregarMedioDeContacto(mailEntidad);
    juridica.getMediosDeContacto().setMedioDeContactoPredeterminado(mailEntidad);
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(null, juridica);

    Mail mailDonante = new Mail("donante@test.com");
    Humana persona = new Humana("Ana", "Perez", 30, 12345678, Genero.OTRO);
    persona.getMediosDeContacto().agregarMedioDeContacto(mailDonante);
    persona.getMediosDeContacto().setMedioDeContactoPredeterminado(mailDonante);
    Donante donante = new Donante(null, persona);

    Donacion donacion = new Donacion();
    donacion.setId(UUID.randomUUID());
    donacion.setEntidad(entidad);
    donacion.setDonante(donante);
    donacion.setSubcategoria(subcategoria);
    donacion.setDescripcion("Leche");
    donacion.setBienes(new ArrayList<>());
    return donacion;
  }

  /**
   * Antes del fix el gestor tenía un constructor con NotificacionesClient; después ya no.
   * Se resuelve por reflexión para que el test compile contra ambas versiones y sirva de
   * regresión real (con el constructor viejo, el cliente recibe 4 avisos).
   */
  private GestorAsignaciones crearGestor(NotificacionesClient client,
                                         RepositorioDeDonaciones repoDonaciones,
                                         RepositorioDeNecesidades repoNecesidades,
                                         FabricaEstrategiasNotificacion fabrica) {
    try {
      return (GestorAsignaciones) GestorAsignaciones.class
          .getConstructor(NotificacionesClient.class, RepositorioDeDonaciones.class,
              RepositorioDeNecesidades.class, FabricaEstrategiasNotificacion.class)
          .newInstance(client, repoDonaciones, repoNecesidades, fabrica);
    } catch (NoSuchMethodException sinCliente) {
      try {
        return GestorAsignaciones.class
            .getConstructor(RepositorioDeDonaciones.class, RepositorioDeNecesidades.class,
                FabricaEstrategiasNotificacion.class)
            .newInstance(repoDonaciones, repoNecesidades, fabrica);
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException("No se pudo construir GestorAsignaciones", e);
      }
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("No se pudo construir GestorAsignaciones", e);
    }
  }
}
