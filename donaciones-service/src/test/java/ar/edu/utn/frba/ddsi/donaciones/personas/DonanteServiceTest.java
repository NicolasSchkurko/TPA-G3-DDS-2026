package ar.edu.utn.frba.ddsi.donaciones.personas;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.personadonante.PersonaDonanteDTO;
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
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.*;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import java.lang.reflect.Field;
import java.util.Map;
import ar.edu.utn.frba.ddsi.donaciones.dto.personadonante.ReporteImportacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;

import org.springframework.data.domain.Pageable;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IDDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.MapeoCSV;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Nested;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import org.springframework.transaction.annotation.Transactional;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorCatalogo;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorIncentivos;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorGeoref;

public class DonanteServiceTest {

  private RepositorioDePersonas repositorioPersonas;
  private GestorCatalogo repositorioCiudades;
  private FabricaEstrategiasNotificacion fabricaEstrategias;
  private IncentivosClient incentivosClient;
  private RepositorioDeDonantes repositorioDonantes;
  private RepositorioDeDonaciones repositorioDonaciones;
  private RepositorioDeFormularios repositorioFormularios;
  private ExecutorService executorImportacion;
  private DonanteService donanteService;

  @BeforeEach
  void setUp() {
    repositorioPersonas = mock(RepositorioDePersonas.class);
    repositorioCiudades = mock(GestorCatalogo.class);
    fabricaEstrategias = mock(FabricaEstrategiasNotificacion.class);
    incentivosClient = mock(IncentivosClient.class);
    repositorioDonantes = mock(RepositorioDeDonantes.class);
    repositorioDonaciones = mock(RepositorioDeDonaciones.class);
    repositorioFormularios = mock(RepositorioDeFormularios.class);
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
  @DisplayName("La baja se encola en la outbox y se intenta después del commit")
  void eliminarPersona_encolaLaBajaEnLaOutbox() {
    GestorIncentivos gestorIncentivos = mock(GestorIncentivos.class);
    UUID donanteId = UUID.randomUUID();
    UUID outboxId = UUID.randomUUID();
    Donante donanteMock = mock(Donante.class);
    when(repositorioDonantes.buscarPorId(donanteId)).thenReturn(Optional.of(donanteMock));
    when(gestorIncentivos.encolarEliminacionPerfil(donanteId)).thenReturn(outboxId);

    DonanteService service = new DonanteService(
        repositorioPersonas,
        repositorioCiudades,
        fabricaEstrategias,
        incentivosClient,
        repositorioDonantes,
        repositorioDonaciones,
        repositorioFormularios,
        new GestorGeoref(null, false),
        executorImportacion,
        gestorIncentivos);

    service.eliminarPersona(donanteId);

    verify(donanteMock).darDeBaja();
    verify(gestorIncentivos).encolarEliminacionPerfil(donanteId);
    verify(gestorIncentivos).intentarAhora(outboxId);
    verify(incentivosClient, never()).eliminarPerfil(any());
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

  @Test
  @DisplayName("Punto 52: crearPersona() persiste localmente Persona y Donante antes de notificar y llamar a incentivos")
  void crearPersona_persisteLocalmentePrimero_luegoNotificaEIncentivos() {
    PersonaDonanteDTO dto = new PersonaDonanteDTO();
    dto.setTipoPersona("HUMANA");
    dto.setNombre("Mario");
    dto.setApellido("Bros");
    dto.setEdad(35);
    dto.setNumeroDeDocumento(11223344);
    dto.setGenero("HOMBRE");

    PersonaDonanteDTO creado = donanteService.crearPersona(dto);

    assertNotNull(creado);
    InOrder inOrder = inOrder(repositorioPersonas, repositorioDonantes, incentivosClient, fabricaEstrategias);
    inOrder.verify(repositorioPersonas).registrarPersona(any());
    inOrder.verify(repositorioDonantes).guardar(any());
    inOrder.verify(incentivosClient).peticionCrearPerfil(any());
    inOrder.verify(fabricaEstrategias).ejecutar(eq(TipoEventoNotificacion.REGISTRO_PERSONA), any());
  }

  @Test
  @DisplayName("Punto 52: Si la persistencia local falla, nunca llama a incentivos ni notifica")
  void crearPersona_siPersistenciaLocalFalla_noLlamaAIncentivosNiNotifica() {
    PersonaDonanteDTO dto = new PersonaDonanteDTO();
    dto.setTipoPersona("HUMANA");
    dto.setNombre("Mario");
    dto.setApellido("Bros");

    doThrow(new IllegalArgumentException("Error simulado de base de datos"))
        .when(repositorioDonantes).guardar(any());

    assertThrows(IllegalArgumentException.class, () -> donanteService.crearPersona(dto));

    verify(incentivosClient, never()).peticionCrearPerfil(any());
    verify(fabricaEstrategias, never()).ejecutar(any(), any());
  }

  @Test
  @DisplayName("Punto 72: buscarPorNombre() delega en repositorioDonantes.buscarPorNombre y no escanea en memoria")
  void buscarPorNombre_delegaEnRepositorio_sinEscanearTodaLaTabla() {
    Donante activo = new Donante();
    activo.setPersona(new Humana("Carlos", "Gomez", 30, 12345678, Genero.HOMBRE));

    when(repositorioDonantes.buscarPorNombre("Carlos Gomez")).thenReturn(Optional.of(activo));

    PersonaDonanteDTO resultado = donanteService.buscarPorNombre("Carlos Gomez");

    assertNotNull(resultado);
    assertEquals("Carlos Gomez", resultado.getNombre() + " " + resultado.getApellido());
    verify(repositorioDonantes).buscarPorNombre("Carlos Gomez");
    verify(repositorioDonantes, never()).obtenerTodos();
  }

  @Test
  @DisplayName("Punto 73: reportesImportacion está acotado por LRU y no supera MAX_REPORTES para evitar fugas de memoria")
  void reportesImportacion_acotadoPorLRU_evitaFugaDeMemoria() throws Exception {
    Field field = DonanteService.class.getDeclaredField("reportesImportacion");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<UUID, ReporteImportacionDTO> map = (Map<UUID, ReporteImportacionDTO>) field.get(donanteService);

    UUID primerId = UUID.randomUUID();
    map.put(primerId, ReporteImportacionDTO.enProgreso(primerId));

    for (int i = 0; i < 105; i++) {
      UUID id = UUID.randomUUID();
      map.put(id, ReporteImportacionDTO.enProgreso(id));
    }

    assertEquals(100, map.size());
    assertNull(map.get(primerId));
  }

  @Test
  @DisplayName("Punto 80: revisarActividades no hace findAll de donantes")
  void revisarActividades_noCargaTodosLosDonantes() {
    Donante donante = new Donante();
    donante.setId(UUID.randomUUID());
    when(repositorioDonantes.obtenerTodos()).thenReturn(List.of(donante));

    donanteService.revisarActividades();

    verify(repositorioDonantes, never()).obtenerTodos();
  }

  @Test
  @DisplayName("Punto 67/80: notifica solo a los inactivos y marca la fecha para no repetir")
  void revisarActividades_notificaYMarcaUltimaNotificacion() {
    Donante donante = new Donante();
    donante.setId(UUID.randomUUID());
    when(repositorioDonantes.buscarInactivosParaNotificar(
        any(LocalDate.class), any(Pageable.class))).thenReturn(List.of(donante));

    donanteService.revisarActividades();

    verify(fabricaEstrategias).ejecutar(
        TipoEventoNotificacion.INACTIVIDAD_PERSONA_DONANTE, donante);
    assertEquals(LocalDate.now(), donante.getUltimaNotificacionInactividad());
    verify(repositorioDonantes, never()).obtenerTodos();
  }

  @Test
  @DisplayName("Una falla al notificar un donante no marca ni corta el resto del lote")
  void revisarActividades_aislaLaFallaPorDonante() {
    Donante primero = new Donante();
    primero.setId(UUID.randomUUID());
    Donante segundo = new Donante();
    segundo.setId(UUID.randomUUID());
    when(repositorioDonantes.buscarInactivosParaNotificar(
        any(LocalDate.class), any(Pageable.class))).thenReturn(List.of(primero, segundo));
    doThrow(new RuntimeException("sin medio"))
        .when(fabricaEstrategias).ejecutar(
            TipoEventoNotificacion.INACTIVIDAD_PERSONA_DONANTE, primero);

    donanteService.revisarActividades();

    assertNull(primero.getUltimaNotificacionInactividad());
    assertEquals(LocalDate.now(), segundo.getUltimaNotificacionInactividad());
    verify(fabricaEstrategias).ejecutar(
        TipoEventoNotificacion.INACTIVIDAD_PERSONA_DONANTE, segundo);
  }

  @Nested
  @DisplayName("Importación en segundo plano")
  class ImportacionSegundoPlano {

    private static final byte[] CSV_MINIMO =
        "TipoPersona,Nombre,Documento\nHUMANA,Ana Navarro,1\n".getBytes(StandardCharsets.UTF_8);

    private RepositorioDePersonas repoPersonasImport;
    private IncentivosClient incentivosImport;

    private List<MapeoCSV> mapeos() {
      return List.of(
          new MapeoCSV("TIPO_PERSONA", List.of("TipoPersona")),
          new MapeoCSV("NOMBRE_RAZON_SOCIAL", List.of("Nombre")),
          new MapeoCSV("DOCUMENTO", List.of("Documento")));
    }

    private DonanteService servicioCon(ExecutorService executor) {
      repoPersonasImport = mock(RepositorioDePersonas.class);
      incentivosImport = mock(IncentivosClient.class);
      return new DonanteService(
          repoPersonasImport,
          mock(GestorCatalogo.class),
          mock(FabricaEstrategiasNotificacion.class),
          incentivosImport,
          mock(RepositorioDeDonantes.class),
          executor);
    }

    private MockMultipartFile archivo() {
      return new MockMultipartFile("file", "donantes.csv", "text/csv", CSV_MINIMO);
    }

    private static ExecutorService poolDeUnHilo() {
      return Executors.newSingleThreadExecutor(tarea -> {
        Thread hilo = new Thread(tarea, "test-importacion");
        hilo.setDaemon(true);
        return hilo;
      });
    }

    @Test
    @DisplayName("Con el pool y la cola llenos, la importación se rechaza en vez de encolarse")
    void colaLLena_rechazaLaImportacion() throws Exception {
      ExecutorService saturado = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
          new ArrayBlockingQueue<>(1),
          tarea -> {
            Thread hilo = new Thread(tarea);
            hilo.setDaemon(true);
            return hilo;
          });
      CountDownLatch liberar = new CountDownLatch(1);
      saturado.execute(() -> esperar(liberar));
      saturado.execute(() -> esperar(liberar)); // ya llena la cola de 1

      try {
        DonanteService servicio = servicioCon(saturado);
        assertThrows(RejectedExecutionException.class,
            () -> servicio.importarDonantes(archivo(), mapeos()),
            "con worker y cola ocupados la importación no puede quedar encolada en silencio");
      } finally {
        liberar.countDown();
        saturado.shutdownNow();
      }
    }

    @Test
    @DisplayName("Un Error en el worker igual deja el reporte COMPLETADO, nunca EN_PROGRESO eterno")
    void unErrorCierraElReporteIgual() throws Exception {
      ExecutorService unHilo = poolDeUnHilo();

      try {
        DonanteService servicio = servicioCon(unHilo);
        doThrow(new StackOverflowError("falla simulada del worker"))
            .when(repoPersonasImport).registrarPersona(any());

        UUID importId = servicio.importarDonantes(archivo(), mapeos());

        ReporteImportacionDTO reporte = esperarCompletado(servicio, importId, Duration.ofSeconds(10));
        assertEquals("COMPLETADO", reporte.getEstado(),
            "el reporte no puede quedar EN_PROGRESO para siempre si el worker muere con un Error");
      } finally {
        unHilo.shutdownNow();
      }
    }

    @Test
    @DisplayName("Punto 78: la importación no llama a MultipartFile.getBytes()")
    void noUsaGetBytes() throws Exception {
      ExecutorService unHilo = poolDeUnHilo();

      try {
        DonanteService servicio = servicioCon(unHilo);
        MockMultipartFile archivo = spy(archivo());
        doThrow(new UnsupportedOperationException("getBytes no debe usarse"))
            .when(archivo).getBytes();

        UUID importId = servicio.importarDonantes(archivo, mapeos());
        esperarCompletado(servicio, importId, Duration.ofSeconds(10));

        verify(archivo, never()).getBytes();
      } finally {
        unHilo.shutdownNow();
      }
    }

    @Test
    @DisplayName("Los perfiles van a incentivos en un solo lote, no uno por fila")
    void losPerfilesVanEnLote() throws Exception {
      ExecutorService unHilo = poolDeUnHilo();

      try {
        DonanteService servicio = servicioCon(unHilo);
        UUID importId = servicio.importarDonantes(archivo(), mapeos());
        esperarCompletado(servicio, importId, Duration.ofSeconds(10));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<IDDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(incentivosImport).peticionCrearPerfilesEnLote(captor.capture());

        List<IDDTO> lote = captor.getValue();
        assertEquals(1, lote.size(), "una fila, un perfil en el lote");
        assertEquals("Ana Navarro", lote.get(0).getNombreUsuario());
        assertEquals("DONANTE", lote.get(0).getRole());
      } finally {
        unHilo.shutdownNow();
      }
    }

    private ReporteImportacionDTO esperarCompletado(DonanteService servicio, UUID importId,
                                                    Duration timeout) throws InterruptedException {
      long limite = System.nanoTime() + timeout.toNanos();
      ReporteImportacionDTO ultimo = null;
      while (System.nanoTime() < limite) {
        ultimo = servicio.obtenerReporteImportacion(importId);
        if ("COMPLETADO".equals(ultimo.getEstado())) {
          return ultimo;
        }
        Thread.sleep(20);
      }
      return fail("El reporte quedó en " + (ultimo == null ? "?" : ultimo.getEstado()));
    }

    private static void esperar(CountDownLatch latch) {
      try {
        latch.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

  }


  @Test
  @DisplayName("revisarActividades debe ser @Transactional")
  void revisarActividadesEsTransactional() throws NoSuchMethodException {
    Method method = DonanteService.class.getMethod("revisarActividades");
    assertTrue(method.isAnnotationPresent(Transactional.class),
        "revisarActividades necesita transacción para los cambios de ultimaNotificacionInactividad");
  }

}
