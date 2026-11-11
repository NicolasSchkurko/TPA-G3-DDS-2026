package ar.edu.utn.frba.ddsi.donaciones.incentivos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IDDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IncentivosDonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.incentivos.NotificacionIncentivosPendiente;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.incentivos.TipoNotificacionIncentivos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeNotificacionesIncentivosPendientes;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorIncentivos;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Nested;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DisplayName("Punto 76: outbox durable de notificaciones a incentivos")
class GestorIncentivosTest {

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  @Test
  @DisplayName("el impacto se persiste en la outbox con su tipo, usuario y payload")
  void encolarImpactoPersisteLaFila() {
    RepositorioDeNotificacionesIncentivosPendientes repo =
        mock(RepositorioDeNotificacionesIncentivosPendientes.class);
    UUID idUsuario = UUID.randomUUID();
    IncentivosDonacionDTO dto = new IncentivosDonacionDTO();
    dto.setIdDonacion(UUID.randomUUID());
    dto.setEstado("ENTREGADA");

    UUID id = gestorCon(repo, mock(IncentivosClient.class)).encolarImpacto(idUsuario, dto);

    ArgumentCaptor<NotificacionIncentivosPendiente> captor =
        ArgumentCaptor.forClass(NotificacionIncentivosPendiente.class);
    verify(repo).save(captor.capture());
    NotificacionIncentivosPendiente pendiente = captor.getValue();
    assertEquals(id, pendiente.getId());
    assertEquals(TipoNotificacionIncentivos.IMPACTO_DONACION, pendiente.getTipo());
    assertEquals(idUsuario, pendiente.getIdUsuario());
    assertEquals(0, pendiente.getIntentos());
    assertTrue(pendiente.getPayloadJson().contains(dto.getIdDonacion().toString()));
  }

  @Test
  @DisplayName("si incentivos responde, la fila se borra")
  void procesarPendientesConfirmaElEnvio() throws Exception {
    RepositorioDeNotificacionesIncentivosPendientes repo =
        mock(RepositorioDeNotificacionesIncentivosPendientes.class);
    IncentivosClient client = mock(IncentivosClient.class);
    UUID idUsuario = UUID.randomUUID();
    NotificacionIncentivosPendiente pendiente = pendienteImpacto(idUsuario);
    when(repo.buscarDisponibles(any(), any())).thenReturn(List.of(pendiente), List.of());

    gestorCon(repo, client).procesarPendientes();

    verify(client).notificarImpactoDonacion(eq(idUsuario), any(IncentivosDonacionDTO.class));
    verify(repo).deleteById(pendiente.getId());
  }

  @Test
  @DisplayName("si incentivos falla, la fila queda y se reprograma con backoff")
  void procesarPendientesReintentaCuandoFalla() {
    RepositorioDeNotificacionesIncentivosPendientes repo =
        mock(RepositorioDeNotificacionesIncentivosPendientes.class);
    IncentivosClient client = mock(IncentivosClient.class);
    UUID idUsuario = UUID.randomUUID();
    NotificacionIncentivosPendiente pendiente = pendienteImpacto(idUsuario);
    LocalDateTime antes = pendiente.getProximoIntento();
    when(repo.buscarDisponibles(any(), any())).thenReturn(List.of(pendiente), List.of());
    when(repo.findById(pendiente.getId())).thenReturn(Optional.of(pendiente));
    doThrow(new RuntimeException("incentivos caído"))
        .when(client).notificarImpactoDonacion(any(), any());

    gestorCon(repo, client).procesarPendientes();

    verify(repo, never()).deleteById(any());
    assertEquals(1, pendiente.getIntentos());
    assertNull(pendiente.getLeaseHasta());
    assertTrue(pendiente.getProximoIntento().isAfter(antes));
    assertEquals("incentivos caído", pendiente.getUltimoError());
  }

  @Test
  @DisplayName("la actualización de perfil también viaja por la outbox")
  void procesarPendientesEnviaActualizacionDePerfil() throws Exception {
    RepositorioDeNotificacionesIncentivosPendientes repo =
        mock(RepositorioDeNotificacionesIncentivosPendientes.class);
    IncentivosClient client = mock(IncentivosClient.class);
    UUID idUsuario = UUID.randomUUID();
    IDDTO dto = new IDDTO(idUsuario, "Sofia Garcia", "DONANTE");
    NotificacionIncentivosPendiente pendiente = new NotificacionIncentivosPendiente(
        TipoNotificacionIncentivos.ACTUALIZACION_PERFIL, idUsuario,
        mapper.writeValueAsString(dto), LocalDateTime.now());
    when(repo.buscarDisponibles(any(), any())).thenReturn(List.of(pendiente), List.of());

    gestorCon(repo, client).procesarPendientes();

    ArgumentCaptor<IDDTO> captor = ArgumentCaptor.forClass(IDDTO.class);
    verify(client).actualizarPerfil(eq(idUsuario), captor.capture());
    assertEquals("Sofia Garcia", captor.getValue().getNombreUsuario());
    verify(repo).deleteById(pendiente.getId());
  }

  @Test
  @DisplayName("el intento inmediato confirma la fila si incentivos responde")
  void intentarAhoraConfirma() {
    RepositorioDeNotificacionesIncentivosPendientes repo =
        mock(RepositorioDeNotificacionesIncentivosPendientes.class);
    IncentivosClient client = mock(IncentivosClient.class);
    UUID idUsuario = UUID.randomUUID();
    NotificacionIncentivosPendiente pendiente = pendienteImpacto(idUsuario);
    when(repo.buscarPorIdDisponible(eq(pendiente.getId()), any()))
        .thenReturn(Optional.of(pendiente));

    gestorCon(repo, client).intentarAhora(pendiente.getId());

    verify(client).notificarImpactoDonacion(eq(idUsuario), any(IncentivosDonacionDTO.class));
    verify(repo).deleteById(pendiente.getId());
  }

  @Test
  @DisplayName("el intento inmediato reprograma si incentivos falla")
  void intentarAhoraReintenta() {
    RepositorioDeNotificacionesIncentivosPendientes repo =
        mock(RepositorioDeNotificacionesIncentivosPendientes.class);
    IncentivosClient client = mock(IncentivosClient.class);
    UUID idUsuario = UUID.randomUUID();
    NotificacionIncentivosPendiente pendiente = pendienteImpacto(idUsuario);
    LocalDateTime antes = pendiente.getProximoIntento();
    when(repo.buscarPorIdDisponible(eq(pendiente.getId()), any()))
        .thenReturn(Optional.of(pendiente));
    when(repo.findById(pendiente.getId())).thenReturn(Optional.of(pendiente));
    doThrow(new RuntimeException("incentivos caído"))
        .when(client).notificarImpactoDonacion(any(), any());

    gestorCon(repo, client).intentarAhora(pendiente.getId());

    verify(repo, never()).deleteById(any());
    assertEquals(1, pendiente.getIntentos());
    assertTrue(pendiente.getProximoIntento().isAfter(antes));
  }

  @Test
  @DisplayName("actualizarPerfil relanza el error para que la outbox lo reintente")
  void actualizarPerfilRelanza() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    doThrow(new ResourceAccessException("caído"))
        .when(restTemplate).put(anyString(), any());
    IncentivosClient client = new IncentivosClient(restTemplate);
    ReflectionTestUtils.setField(client, "incentivosUrl", "http://localhost:8082");

    assertThrows(ResourceAccessException.class,
        () -> client.actualizarPerfil(UUID.randomUUID(), new IDDTO()));
  }

  @Test
  @DisplayName("encolarEliminacionPerfil persiste el tipo y el usuario, sin payload")
  void encolarEliminacionPersisteElTipo() {
    RepositorioDeNotificacionesIncentivosPendientes repo =
        mock(RepositorioDeNotificacionesIncentivosPendientes.class);
    UUID idUsuario = UUID.randomUUID();

    gestorCon(repo, mock(IncentivosClient.class)).encolarEliminacionPerfil(idUsuario);

    ArgumentCaptor<NotificacionIncentivosPendiente> captor =
        ArgumentCaptor.forClass(NotificacionIncentivosPendiente.class);
    verify(repo).save(captor.capture());
    assertEquals(TipoNotificacionIncentivos.ELIMINAR_PERFIL, captor.getValue().getTipo());
    assertEquals(idUsuario, captor.getValue().getIdUsuario());
  }

  @Test
  @DisplayName("la baja de perfil también se procesa desde la outbox")
  void procesarPendientesEnviaEliminacionDePerfil() throws Exception {
    RepositorioDeNotificacionesIncentivosPendientes repo =
        mock(RepositorioDeNotificacionesIncentivosPendientes.class);
    IncentivosClient client = mock(IncentivosClient.class);
    UUID idUsuario = UUID.randomUUID();
    NotificacionIncentivosPendiente pendiente = new NotificacionIncentivosPendiente(
        TipoNotificacionIncentivos.ELIMINAR_PERFIL, idUsuario, "{}", LocalDateTime.now());
    when(repo.buscarDisponibles(any(), any())).thenReturn(List.of(pendiente), List.of());

    gestorCon(repo, client).procesarPendientes();

    verify(client).eliminarPerfil(idUsuario);
    verify(repo).deleteById(pendiente.getId());
  }

  private GestorIncentivos gestorCon(RepositorioDeNotificacionesIncentivosPendientes repo,
                                     IncentivosClient client) {
    return new GestorIncentivos(repo, client, mapper, mock(PlatformTransactionManager.class));
  }

  private NotificacionIncentivosPendiente pendienteImpacto(UUID idUsuario) {
    IncentivosDonacionDTO dto = new IncentivosDonacionDTO();
    dto.setIdDonacion(UUID.randomUUID());
    dto.setEstado("ENTREGADA");
    try {
      return new NotificacionIncentivosPendiente(
          TipoNotificacionIncentivos.IMPACTO_DONACION, idUsuario,
          mapper.writeValueAsString(dto), LocalDateTime.now());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Nested
  @DisplayName("Transacciones")
  class Transacciones {

    @Test
    @DisplayName("encolar sigue la transacción de la entrega o del PUT")
    void encolarSigueLaTransaccionDelLlamador() throws Exception {
      assertPropagation("encolarImpacto", Propagation.REQUIRED,
          UUID.class, IncentivosDonacionDTO.class);
      assertPropagation("encolarActualizacionPerfil", Propagation.REQUIRED,
          UUID.class, IDDTO.class);
    }

    @Test
    @DisplayName("el claim, el confirm y el retry usan una transacción nueva (afterCommit no commitea)")
    void operacionesDeOutboxUsanRequiresNew() throws Exception {
      GestorIncentivos gestor = new GestorIncentivos(
          mock(RepositorioDeNotificacionesIncentivosPendientes.class),
          mock(IncentivosClient.class),
          new ObjectMapper(),
          mock(PlatformTransactionManager.class));

      Field field = GestorIncentivos.class.getDeclaredField("requiereNuevaTransaccion");
      field.setAccessible(true);
      TransactionTemplate template = (TransactionTemplate) field.get(gestor);

      assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW,
          template.getPropagationBehavior());
    }

    private void assertPropagation(String metodo, Propagation esperada, Class<?>... parametros)
        throws Exception {
      Method method = GestorIncentivos.class.getMethod(metodo, parametros);
      Transactional transactional = method.getAnnotation(Transactional.class);
      assertNotNull(transactional, metodo + " debe ser @Transactional");
      assertEquals(esperada, transactional.propagation(), metodo);
    }
  }

}
