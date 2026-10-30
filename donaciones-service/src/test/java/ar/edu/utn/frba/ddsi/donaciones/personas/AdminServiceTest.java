package ar.edu.utn.frba.ddsi.donaciones.personas;

import ar.edu.utn.frba.ddsi.donaciones.dto.admin.AdminDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Whatsapp;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.AdministradorJpaRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioAdministradores;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioPersonas;
import ar.edu.utn.frba.ddsi.donaciones.services.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AdminServiceTest {

  private RepositorioAdministradores repositorioAdministradores;
  private RepositorioPersonas repositorioPersonas;
  private AdminService adminService;

  @BeforeEach
  void setUp() {
    repositorioAdministradores = mock(RepositorioAdministradores.class);
    repositorioPersonas = mock(RepositorioPersonas.class);
    adminService = new AdminService(repositorioAdministradores, repositorioPersonas);
  }

  @Test
  @DisplayName("Punto 46: PUT /api/admins actualiza la entidad existente preservando el ID original sin crear un nuevo registro")
  void actualizarAdmin_preservaIdOriginalYActualizaEntidadExistente() {
    UUID idOriginal = UUID.randomUUID();
    Humana humanoExistente = new Humana("Carlos", "Gomez", 35, 30123456, Genero.HOMBRE);
    Whatsapp contactoExistente = new Whatsapp("1122334455");
    Administrador adminExistente = new Administrador(humanoExistente, contactoExistente, "Carlos Admin");
    adminExistente.setId(idOriginal);

    when(repositorioAdministradores.buscarPorId(idOriginal)).thenReturn(Optional.of(adminExistente));

    AdminDTO dtoModificado = new AdminDTO();
    dtoModificado.setNombreAMostrar("Carlos Modificado");
    dtoModificado.setNombre("Carlos");
    dtoModificado.setApellido("Gomez Actualizado");
    dtoModificado.setEdad(36);
    dtoModificado.setNumeroDeDocumento(30123456);
    dtoModificado.setGenero("HOMBRE");
    MediosContactoDTO contactoDTO = new MediosContactoDTO();
    contactoDTO.setTipo("WHATSAPP");
    contactoDTO.setValor("1199887766");
    dtoModificado.setMedioDeContacto(contactoDTO);

    AdminDTO resultado = adminService.actualizarAdmin(idOriginal, dtoModificado);

    assertNotNull(resultado);
    assertEquals(idOriginal.toString(), resultado.getId());
    assertEquals("Carlos Modificado", resultado.getNombreAMostrar());

    ArgumentCaptor<Administrador> adminCaptor = ArgumentCaptor.forClass(Administrador.class);
    verify(repositorioAdministradores, times(1)).actualizar(eq(idOriginal), adminCaptor.capture());

    Administrador actualizado = adminCaptor.getValue();
    assertEquals(idOriginal, actualizado.getId(), "El administrador enviado al repositorio debe conservar el ID original");
    assertEquals("Carlos Modificado", actualizado.getNombreAMostrar());
  }

  @Test
  @DisplayName("Punto 46: RepositorioAdministradores.actualizar fuerza el ID original antes de save en caso de recibir objeto nuevo")
  void repositorioAdministradores_actualizar_asignaIdOriginalAntesDeGuardar() {
    AdministradorJpaRepository jpaRepo = mock(AdministradorJpaRepository.class);
    RepositorioAdministradores repo = new RepositorioAdministradores(jpaRepo);

    UUID idOriginal = UUID.randomUUID();
    when(jpaRepo.existsById(idOriginal)).thenReturn(true);

    Administrador nuevoObjetoConOtroId = new Administrador(
        new Humana("Test", "Admin", 40, 12345678, Genero.OTRO),
        new Whatsapp("11223344"),
        "Test Admin"
    );

    repo.actualizar(idOriginal, nuevoObjetoConOtroId);

    ArgumentCaptor<Administrador> captor = ArgumentCaptor.forClass(Administrador.class);
    verify(jpaRepo, times(1)).save(captor.capture());

    Administrador guardado = captor.getValue();
    assertEquals(idOriginal, guardado.getId(), "Debe reasignar el idOriginal antes de ejecutar save para que Hibernate haga UPDATE y no INSERT");
  }
}
