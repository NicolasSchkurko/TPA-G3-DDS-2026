package ar.edu.utn.frba.ddsi.donaciones.identidad;

import ar.edu.utn.frba.ddsi.donaciones.dto.identidad.IdentidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioAdministradores;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioDonantes;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.services.IdentidadService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IdentidadServiceTest {

  private static final String SUB = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
  private static final String OTRO_SUB = "b2c3d4e5-f6a7-8901-bcde-f01234567891";

  private RepositorioEntidadesBeneficiarias repositorioEntidades;
  private RepositorioDonantes repositorioDonantes;
  private RepositorioAdministradores repositorioAdministradores;
  private IdentidadService service;

  @BeforeEach
  void setUp() {
    repositorioEntidades = Mockito.mock(RepositorioEntidadesBeneficiarias.class);
    repositorioDonantes = Mockito.mock(RepositorioDonantes.class);
    repositorioAdministradores = Mockito.mock(RepositorioAdministradores.class);
    service = new IdentidadService(repositorioEntidades, repositorioDonantes, repositorioAdministradores);
  }

  @Test
  void porSubDevuelveLosPerfilesVinculadosYLosRoles() {
    Donante donante = new Donante();
    EntidadBeneficiaria entidad = new EntidadBeneficiaria();
    when(repositorioDonantes.buscarPorKeycloakSub(SUB)).thenReturn(Optional.of(donante));
    when(repositorioEntidades.buscarPorKeycloakSub(SUB)).thenReturn(Optional.of(entidad));
    when(repositorioAdministradores.buscarPorKeycloakSub(SUB)).thenReturn(Optional.empty());

    IdentidadDTO dto = service.porSub(SUB);

    assertEquals(donante.getId(), dto.getDonanteId());
    assertEquals(entidad.getId(), dto.getEntidadId());
    assertNull(dto.getAdminId());
    assertEquals(java.util.List.of("ENTIDAD", "DONANTE"), dto.getRoles());
  }

  @Test
  void porSubRechazaUnIdentificadorInvalido() {
    assertThrows(IllegalArgumentException.class, () -> service.porSub("no-es-un-uuid"));
    assertThrows(IllegalArgumentException.class, () -> service.porSub(null));
    assertThrows(IllegalArgumentException.class, () -> service.porSub("  "));
  }

  @Test
  void vincularAsignaElSubAlPerfil() {
    Donante donante = new Donante();
    UUID id = donante.getId();
    when(repositorioDonantes.buscarPorId(id)).thenReturn(Optional.of(donante));
    when(repositorioDonantes.buscarPorKeycloakSub(SUB)).thenReturn(Optional.empty());

    service.vincular(SUB, "DONANTE", id);

    assertEquals(SUB, donante.getKeycloakSub());
    verify(repositorioDonantes).actualizar(id, donante);
  }

  @Test
  void vincularEsIdempotenteCuandoYaEstaVinculadoConElMismoSub() {
    Donante donante = new Donante();
    donante.setKeycloakSub(SUB);
    UUID id = donante.getId();
    when(repositorioDonantes.buscarPorId(id)).thenReturn(Optional.of(donante));
    when(repositorioDonantes.buscarPorKeycloakSub(SUB)).thenReturn(Optional.of(donante));

    service.vincular(SUB, "DONANTE", id);

    assertEquals(SUB, donante.getKeycloakSub());
    verify(repositorioDonantes).actualizar(id, donante);
  }

  @Test
  void vincularRechazaUnPerfilYaVinculadoAOTRaCuenta() {
    Donante donante = new Donante();
    donante.setKeycloakSub(OTRO_SUB);
    UUID id = donante.getId();
    when(repositorioDonantes.buscarPorId(id)).thenReturn(Optional.of(donante));

    assertThrows(IllegalStateException.class, () -> service.vincular(SUB, "DONANTE", id));
    verify(repositorioDonantes, never()).actualizar(any(), any());
  }

  @Test
  void vincularRechazaUnaCuentaYaVinculadaAOTROPerfilDelMismoRol() {
    Donante perfil = new Donante();
    Donante otro = new Donante();
    UUID id = perfil.getId();
    when(repositorioDonantes.buscarPorId(id)).thenReturn(Optional.of(perfil));
    when(repositorioDonantes.buscarPorKeycloakSub(SUB)).thenReturn(Optional.of(otro));

    assertThrows(IllegalStateException.class, () -> service.vincular(SUB, "DONANTE", id));
    verify(repositorioDonantes, never()).actualizar(any(), any());
  }

  @Test
  void vincularRechazaRolInvalidoOPerfilInexistente() {
    assertThrows(IllegalArgumentException.class, () -> service.vincular(SUB, "CHOFER", UUID.randomUUID()));
    assertThrows(IllegalArgumentException.class, () -> service.vincular(SUB, null, UUID.randomUUID()));
    assertThrows(IllegalArgumentException.class, () -> service.vincular(SUB, "DONANTE", null));

    UUID inexistente = UUID.randomUUID();
    when(repositorioDonantes.buscarPorId(inexistente)).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class, () -> service.vincular(SUB, "DONANTE", inexistente));
  }
}
