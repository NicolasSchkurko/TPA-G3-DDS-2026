package ar.edu.utn.frba.ddsi.donaciones.personas;

import ar.edu.utn.frba.ddsi.donaciones.dto.admin.AdminDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Whatsapp;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeAdministradores;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDePersonas;
import ar.edu.utn.frba.ddsi.donaciones.services.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AdminServiceTest {

  private RepositorioDeAdministradores repositorioAdministradores;
  private RepositorioDePersonas repositorioPersonas;
  private AdminService adminService;

  @BeforeEach
  void setUp() {
    repositorioAdministradores = mock(RepositorioDeAdministradores.class);
    repositorioPersonas = mock(RepositorioDePersonas.class);
    adminService = new AdminService(repositorioAdministradores, repositorioPersonas);
  }

  @Test
  @DisplayName("Punto 65: el PUT conserva la Humana existente y no deja una persona huérfana")
  void actualizarAdmin_conservaLaHumanaExistente() {
    UUID adminId = UUID.randomUUID();
    Humana humanoExistente = new Humana("Carlos", "Gomez", 35, 30123456, Genero.HOMBRE);
    Administrador adminExistente = new Administrador(
        humanoExistente, new Whatsapp("1122334455"), "Carlos Admin");
    adminExistente.setId(adminId);

    when(repositorioAdministradores.buscarPorId(adminId)).thenReturn(Optional.of(adminExistente));

    AdminDTO dto = new AdminDTO();
    dto.setNombreAMostrar("Carlos Modificado");
    dto.setNombre("Carlos");
    dto.setApellido("Gomez Actualizado");
    dto.setEdad(36);
    dto.setNumeroDeDocumento(30123456);
    dto.setGenero("HOMBRE");
    MediosContactoDTO contacto = new MediosContactoDTO();
    contacto.setTipo("WHATSAPP");
    contacto.setValor("1199887766");
    dto.setMedioDeContacto(contacto);

    adminService.actualizarAdmin(adminId, dto);

    ArgumentCaptor<Administrador> captor = ArgumentCaptor.forClass(Administrador.class);
    verify(repositorioAdministradores).actualizar(eq(adminId), captor.capture());
    assertSame(humanoExistente, captor.getValue().getHumano(),
        "no debe reemplazarse la Humana existente por la instancia nueva del DTO");
    verify(repositorioPersonas).modificarPersona(eq(humanoExistente.getId()), any(Humana.class));
    verify(repositorioPersonas, never()).registrarPersona(any());
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
  @DisplayName("El alta usa el id de Keycloak que recibe en el DTO")
  void crearAdmin_usaElIdDeKeycloak() {
    UUID idKeycloak = UUID.randomUUID();
    AdminDTO dto = new AdminDTO();
    dto.setId(idKeycloak.toString());
    dto.setNombreAMostrar("Admin Keycloak");
    dto.setNombre("Ana");
    dto.setApellido("Admin");
    dto.setEdad(30);
    dto.setNumeroDeDocumento(30123456);
    dto.setGenero("MUJER");
    MediosContactoDTO contacto = new MediosContactoDTO();
    contacto.setTipo("WHATSAPP");
    contacto.setValor("1122334455");
    dto.setMedioDeContacto(contacto);

    AdminDTO resultado = adminService.crearAdministrador(dto);

    assertEquals(idKeycloak.toString(), resultado.getId());
    verify(repositorioPersonas).registrarPersona(any(Humana.class));
    ArgumentCaptor<Administrador> captor = ArgumentCaptor.forClass(Administrador.class);
    verify(repositorioAdministradores).guardar(captor.capture());
    assertEquals(idKeycloak, captor.getValue().getId());
  }

}
