package ar.edu.utn.frba.ddsi.donaciones.personas;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.PersonaDonanteDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.*;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DonanteServiceTest {

  private RepositorioPersonas repositorioPersonas;
  private RepositorioCiudades repositorioCiudades;
  private FabricaEstrategiasNotificacion fabricaEstrategias;
  private IncentivosClient incentivosClient;
  private RepositorioDonantes repositorioDonantes;
  private RepositorioDonaciones repositorioDonaciones;
  private RepositorioFormularios repositorioFormularios;
  private ExecutorService executorImportacion;
  private DonanteService donanteService;

  @BeforeEach
  void setUp() {
    repositorioPersonas = mock(RepositorioPersonas.class);
    repositorioCiudades = mock(RepositorioCiudades.class);
    fabricaEstrategias = mock(FabricaEstrategiasNotificacion.class);
    incentivosClient = mock(IncentivosClient.class);
    repositorioDonantes = mock(RepositorioDonantes.class);
    repositorioDonaciones = mock(RepositorioDonaciones.class);
    repositorioFormularios = mock(RepositorioFormularios.class);
    executorImportacion = mock(ExecutorService.class);

    donanteService = new DonanteService(
        repositorioPersonas,
        repositorioCiudades,
        fabricaEstrategias,
        incentivosClient,
        repositorioDonantes,
        repositorioDonaciones,
        repositorioFormularios,
        executorImportacion
    );
  }

  @Test
  @DisplayName("Punto 69: Dar de baja un donante aplica soft delete y anonimización sin desvincular donaciones, y luego notifica incentivos")
  void eliminarPersona_aplicaSoftDeleteYAnonimizacion_yLuegoNotificaIncentivos() {
    UUID donanteId = UUID.randomUUID();
    Donante donanteMock = mock(Donante.class);
    when(repositorioDonantes.buscarPorId(donanteId)).thenReturn(Optional.of(donanteMock));

    donanteService.eliminarPersona(donanteId);

    // Verificar orden estricto de ejecución
    InOrder inOrder = inOrder(donanteMock, repositorioDonantes, incentivosClient);
    inOrder.verify(donanteMock).darDeBaja();
    inOrder.verify(repositorioDonantes).actualizar(donanteId, donanteMock);
    inOrder.verify(repositorioDonantes).flush();
    inOrder.verify(incentivosClient).eliminarPerfil(donanteId);

    // Punto 69: NUNCA se desvinculan las donaciones ni formularios, ni se borra físicamente el donante
    verify(repositorioDonaciones, never()).desvincularDonante(any());
    verify(repositorioFormularios, never()).desvincularDonante(any());
    verify(repositorioDonantes, never()).eliminarPorId(any());
  }

  @Test
  @DisplayName("Punto 35 y 69: Si incentivos-service falla o está caído, el soft delete local no se aborta ni se interrumpe")
  void eliminarPersona_siIncentivosFalla_noAbortaBajaLocal() {
    UUID donanteId = UUID.randomUUID();
    Donante donanteMock = mock(Donante.class);
    when(repositorioDonantes.buscarPorId(donanteId)).thenReturn(Optional.of(donanteMock));

    doThrow(new RestClientException("503 Service Unavailable"))
        .when(incentivosClient).eliminarPerfil(donanteId);

    assertDoesNotThrow(() -> donanteService.eliminarPersona(donanteId));

    verify(donanteMock).darDeBaja();
    verify(repositorioDonantes).actualizar(donanteId, donanteMock);
    verify(repositorioDonantes).flush();
    verify(repositorioDonantes, never()).eliminarPorId(any());
    verify(repositorioDonaciones, never()).desvincularDonante(any());
    verify(repositorioFormularios, never()).desvincularDonante(any());
  }

  @Test
  @DisplayName("Punto 35 y 69: Si el donante no existe, no intenta actualizar, ni desvincular, ni llama a incentivos")
  void eliminarPersona_siDonanteNoExiste_noLlamaAIncentivosNiBorra() {
    UUID donanteId = UUID.randomUUID();
    when(repositorioDonantes.buscarPorId(donanteId)).thenReturn(Optional.empty());

    donanteService.eliminarPersona(donanteId);

    verify(repositorioDonaciones, never()).desvincularDonante(any());
    verify(repositorioFormularios, never()).desvincularDonante(any());
    verify(repositorioDonantes, never()).actualizar(any(), any());
    verify(repositorioDonantes, never()).eliminarPorId(any());
    verify(incentivosClient, never()).eliminarPerfil(any());
  }

  @Test
  @DisplayName("Punto 69: listarTodas() excluye a los donantes dados de baja (activo = false)")
  void listarTodas_excluyeDonantesInactivos() {
    Donante activo = new Donante();
    activo.setPersona(new Humana("Carlos", "Gomez", 30, 12345678, Genero.HOMBRE));

    Donante inactivo = new Donante();
    inactivo.setPersona(new Humana("Ana", "Lopez", 25, 87654321, Genero.MUJER));
    inactivo.darDeBaja();

    when(repositorioDonantes.obtenerTodos()).thenReturn(List.of(activo, inactivo));

    List<PersonaDonanteDTO> resultado = donanteService.listarTodas();

    assertEquals(1, resultado.size());
    assertEquals(activo.getId(), resultado.get(0).getId());
    assertTrue(resultado.get(0).getActivo());
  }

  @Test
  @DisplayName("Punto 69: buscarPorNombre() no retorna donantes dados de baja (activo = false)")
  void buscarPorNombre_ignoraDonantesInactivos() {
    Donante inactivo = new Donante();
    inactivo.setPersona(new Humana("Carlos", "Gomez", 30, 12345678, Genero.HOMBRE));
    inactivo.darDeBaja(); // persona anonimizada, usuario null

    when(repositorioDonantes.obtenerTodos()).thenReturn(List.of(inactivo));

    PersonaDonanteDTO resultado = donanteService.buscarPorNombre("Carlos Gomez");
    assertNull(resultado);
  }

  @Test
  @DisplayName("Punto 69: darDeBaja() en Donante aplica soft delete y anonimiza Persona y Direccion")
  void darDeBaja_integraDonanteYAnonimizacion() {
    Humana persona = new Humana("Juan", "Perez", 35, 23456789, Genero.HOMBRE);
    MediosDeContacto medios = new MediosDeContacto();
    medios.agregarMedioDeContacto(new Mail("juan@correo.com"));
    persona.setMediosDeContacto(medios);

    Pais pais = new Pais("Argentina");
    Provincia provincia = new Provincia("Buenos Aires", pais);
    Ciudad ciudad = new Ciudad("CABA", provincia);
    Direccion direccion = new Direccion("Av. Corrientes", "Callao", 1234, 3, "B", ciudad);

    Donante donante = new Donante(direccion, persona);

    assertTrue(donante.isActivo());
    assertNull(donante.getFechaBaja());

    donante.darDeBaja();

    assertFalse(donante.isActivo());
    assertNotNull(donante.getFechaBaja());

    // Verificación de datos sensibles anonimizados
    assertNull(persona.getNombre());
    assertNull(persona.getApellido());
    assertEquals(0, persona.getEdad());
    assertEquals(0, persona.getNumeroDeDocumento());
    assertNull(persona.getGenero());
    assertNull(persona.getNombreDeUsuario());
    assertTrue(persona.getMediosDeContacto().getListaMediosDeContacto().isEmpty());

    // Verificación de dirección anonimizada
    assertNull(direccion.getCalleUno());
    assertNull(direccion.getCalleDos());
    assertNull(direccion.getAltura());
    assertNull(direccion.getDepartamento());
    assertEquals(0, direccion.getPiso());
    assertEquals(ciudad, direccion.getCiudad()); // catálogo geográfico preservado
  }
}
