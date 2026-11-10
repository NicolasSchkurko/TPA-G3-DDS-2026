package ar.edu.utn.frba.ddsi.donaciones.geocoding;

import ar.edu.utn.frba.ddsi.donaciones.clients.GeorefClient;
import ar.edu.utn.frba.ddsi.donaciones.clients.GeorefException;
import ar.edu.utn.frba.ddsi.donaciones.dto.georef.GeorefDTOs;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorGeoref;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.DireccionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Punto 75: geocoding contra Georef.
 *
 * <p>Se prueba el gestor aislado del HTTP: el cliente se mockea. Lo que importa es que no se
 * consulte de más (direcciones ya resueltas o repetidas), que los lotes respeten el máximo de
 * 1000 y que una caída de Georef no se confunda con "la dirección no existe".
 */
class GestorGeorefTest {

    private GeorefClient georef;
    private GestorGeoref gestor;

    @BeforeEach
    void setUp() {
        georef = mock(GeorefClient.class);
        gestor = new GestorGeoref(georef, true);
    }

    @Test
    @DisplayName("En el alta geocodifica si todavía no hay coordenadas")
    void altaGeocodifica() {
        when(georef.buscar(anyList())).thenReturn(List.of(resultado(-34.6037, -58.3816)));
        Direccion direccion = direccion("Av. Medrano", 951, "Buenos Aires");

        gestor.geocodificarAlta(direccion);

        assertTrue(direccion.tieneCoordenadas());
        assertEquals(-34.6037, direccion.getLatitud());
        assertEquals(-58.3816, direccion.getLongitud());
    }

    @Test
    @DisplayName("Con la integración apagada el alta no consulta Georef")
    void altaConIntegracionApagadaNoConsulta() {
        GestorGeoref apagado = new GestorGeoref(georef, false);

        apagado.geocodificarAlta(direccion("Av. Medrano", 951, "Buenos Aires"));

        verifyNoInteractions(georef);
    }

    @Test
    @DisplayName("Si la dirección no cambió conserva coordenadas y backoff sin llamar a Georef")
    void actualizacionSinCambiosConservaTodo() {
        LocalDateTime ultimoIntento = LocalDateTime.now().minusDays(2);
        Direccion existente = direccion("Av. Medrano", 951, "Buenos Aires");
        existente.setLatitud(-34.6);
        existente.setLongitud(-58.4);
        existente.setIntentosGeocoding(2);
        existente.setUltimoIntentoGeocoding(ultimoIntento);

        Direccion nueva = direccion("  av. medrano ", 951, "Buenos Aires");
        gestor.geocodificarActualizacion(nueva, existente);

        assertEquals(-34.6, nueva.getLatitud());
        assertEquals(-58.4, nueva.getLongitud());
        assertEquals(2, nueva.getIntentosGeocoding());
        assertEquals(ultimoIntento, nueva.getUltimoIntentoGeocoding());
        verifyNoInteractions(georef);
    }

    @Test
    @DisplayName("Si cambió la dirección vuelve a geocodificar")
    void actualizacionConCambiosGeocodifica() {
        when(georef.buscar(anyList())).thenReturn(List.of(resultado(-34.6, -58.4)));
        Direccion existente = direccion("Av. Medrano", 951, "Buenos Aires");
        existente.setLatitud(-34.6);
        existente.setLongitud(-58.4);

        Direccion nueva = direccion("Av. Medrano", 1000, "Buenos Aires");
        gestor.geocodificarActualizacion(nueva, existente);

        verify(georef).buscar(anyList());
        assertEquals(-34.6, nueva.getLatitud());
        assertEquals(-58.4, nueva.getLongitud());
    }

    @Test
    @DisplayName("Si el request ya trae coordenadas no consulta Georef")
    void actualizacionConCoordenadasDelRequestNoConsulta() {
        Direccion nueva = direccion("Av. Medrano", 951, "Buenos Aires");
        nueva.setLatitud(-34.6);
        nueva.setLongitud(-58.4);

        gestor.geocodificarActualizacion(nueva, direccion("Otra calle", 100, "Buenos Aires"));

        verifyNoInteractions(georef);
    }

    @Test
    @DisplayName("No consulta Georef si la dirección ya tiene coordenadas")
    void noConsultaSiYaTieneCoordenadas() {
        Direccion direccion = direccion("Av. Medrano", 951, "Buenos Aires");
        direccion.setLatitud(-34.6);
        direccion.setLongitud(-58.4);

        assertTrue(gestor.completarCoordenadas(direccion));

        verifyNoInteractions(georef);
    }

    @Test
    @DisplayName("Deduplica direcciones iguales: una sola consulta para todas")
    void deduplicaDireccionesIguales() {
        when(georef.buscar(anyList())).thenReturn(List.of(resultado(-34.6, -58.4)));
        Direccion una = direccion("Av. Medrano", 951, "Buenos Aires");
        Direccion otra = direccion("  av. medrano ", 951, "Buenos Aires");

        assertTrue(gestor.completarCoordenadas(List.of(una, otra)));

        ArgumentCaptor<List<GeorefDTOs.Consulta>> captor = captorDeConsultas();
        verify(georef).buscar(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(-34.6, una.getLatitud());
        assertEquals(-34.6, otra.getLatitud());
    }

    @Test
    @DisplayName("Sin resultado no asigna coordenadas pero no es un fallo de infraestructura")
    void sinResultadoNoAsignaYDevuelveTrue() {
        when(georef.buscar(anyList()))
                .thenReturn(List.of(new GeorefDTOs.Resultado(List.of())));

        Direccion direccion = direccion("Calle Inexistente", 1, "Buenos Aires");

        assertTrue(gestor.completarCoordenadas(direccion));

        assertFalse(direccion.tieneCoordenadas());
        // Dos llamadas: la original con localidad y el reintento sin localidad.
        verify(georef, times(2)).buscar(anyList());
    }

    @Test
    @DisplayName("Si Georef falla no rompe el alta: devuelve false y deja las coordenadas nulas")
    void falloDeInfraestructuraDevuelveFalse() {
        when(georef.buscar(anyList()))
                .thenThrow(new GeorefException("caído", new RuntimeException("timeout")));

        Direccion direccion = direccion("Av. Medrano", 951, "Buenos Aires");

        assertFalse(gestor.completarCoordenadas(direccion));
        assertNull(direccion.getLatitud());
        assertNull(direccion.getLongitud());
    }

    @Test
    @DisplayName("Reintenta sin localidad cuando el nombre de ciudad no coincide con el de Georef")
    void reintentaSinLocalidad() {
        when(georef.buscar(anyList()))
                .thenReturn(List.of(new GeorefDTOs.Resultado(List.of())))
                .thenReturn(List.of(resultado(-34.6, -58.4)));

        Direccion direccion = direccion("Av. Medrano", 951, "CABA");

        assertTrue(gestor.completarCoordenadas(direccion));
        assertTrue(direccion.tieneCoordenadas());

        ArgumentCaptor<List<GeorefDTOs.Consulta>> captor = captorDeConsultas();
        verify(georef, times(2)).buscar(captor.capture());
        assertNull(captor.getAllValues().get(1).get(0).localidad());
        assertEquals("CABA", captor.getAllValues().get(0).get(0).localidad());
    }

    @Test
    @DisplayName("Parte más de 1000 direcciones en varios POST, el máximo de Georef")
    void parteLotesDeMasDeMil() {
        when(georef.buscar(anyList())).thenAnswer(invocacion -> {
            List<GeorefDTOs.Consulta> consultas = invocacion.getArgument(0);
            return consultas.stream()
                    .map(consulta -> resultado(-34.6, -58.4))
                    .toList();
        });

        List<Direccion> direcciones = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            direcciones.add(direccion("Calle " + i, i, "Buenos Aires"));
        }

        assertTrue(gestor.completarCoordenadas(direcciones));

        ArgumentCaptor<List<GeorefDTOs.Consulta>> captor = captorDeConsultas();
        verify(georef, times(2)).buscar(captor.capture());
        assertEquals(1000, captor.getAllValues().get(0).size());
        assertEquals(1, captor.getAllValues().get(1).size());
        assertTrue(direcciones.stream().allMatch(Direccion::tieneCoordenadas));
    }

    @Test
    @DisplayName("Con la integración apagada no consulta Georef")
    void deshabilitadoNoConsulta() {
        GestorGeoref apagado = new GestorGeoref(georef, false);
        Direccion direccion = direccion("Av. Medrano", 951, "Buenos Aires");

        assertTrue(apagado.completarCoordenadas(direccion));
        assertFalse(direccion.tieneCoordenadas());
        verify(georef, never()).buscar(anyList());
    }

    @Test
    @DisplayName("Un fallo de Georef no consume intentos: el cron lo reintenta al día siguiente")
    void falloDeGeorefNoConsumeIntentos() {
        DireccionJpaRepository repositorio = mock(DireccionJpaRepository.class);
        GestorGeoref conRepositorio = gestorConRepositorio(repositorio, true);

        Direccion actual = direccionPersistida();
        Direccion resultado = direccion("Av. Medrano", 951, "Buenos Aires");
        resultado.setId(actual.getId());
        when(repositorio.buscarParaActualizar(anyList())).thenReturn(List.of(actual));

        conRepositorio.guardarResultados(List.of(resultado), false);

        assertEquals(0, actual.getIntentosGeocoding());
        assertNull(actual.getUltimoIntentoGeocoding());
    }

    @Test
    @DisplayName("Una dirección sin resultado consume un intento y respeta el backoff")
    void sinResultadoConsumeIntento() {
        DireccionJpaRepository repositorio = mock(DireccionJpaRepository.class);
        GestorGeoref conRepositorio = gestorConRepositorio(repositorio, true);

        Direccion actual = direccionPersistida();
        Direccion resultado = direccion("Calle Inexistente", 1, "Buenos Aires");
        resultado.setId(actual.getId());
        when(repositorio.buscarParaActualizar(anyList())).thenReturn(List.of(actual));

        conRepositorio.guardarResultados(List.of(resultado), true);

        assertEquals(1, actual.getIntentosGeocoding());
        assertNotNull(actual.getUltimoIntentoGeocoding());
    }

    @Test
    @DisplayName("Guarda las coordenadas resueltas y resetea los intentos")
    void guardaCoordenadasResueltas() {
        DireccionJpaRepository repositorio = mock(DireccionJpaRepository.class);
        GestorGeoref conRepositorio = gestorConRepositorio(repositorio, true);

        Direccion actual = direccionPersistida();
        actual.setIntentosGeocoding(2);
        Direccion resultado = direccion("Av. Medrano", 951, "Buenos Aires");
        resultado.setId(actual.getId());
        resultado.setLatitud(-34.6);
        resultado.setLongitud(-58.4);
        when(repositorio.buscarParaActualizar(anyList())).thenReturn(List.of(actual));

        conRepositorio.guardarResultados(List.of(resultado), true);

        assertEquals(-34.6, actual.getLatitud());
        assertEquals(-58.4, actual.getLongitud());
        assertEquals(0, actual.getIntentosGeocoding());
    }

    @Test
    @DisplayName("No pisa coordenadas que otra instancia ya había guardado")
    void noPisaCoordenadasDeOtraInstancia() {
        DireccionJpaRepository repositorio = mock(DireccionJpaRepository.class);
        GestorGeoref conRepositorio = gestorConRepositorio(repositorio, true);

        Direccion actual = direccionPersistida();
        actual.setLatitud(-34.1);
        actual.setLongitud(-58.1);
        Direccion resultado = direccion("Av. Medrano", 951, "Buenos Aires");
        resultado.setId(actual.getId());
        resultado.setLatitud(-34.9);
        resultado.setLongitud(-58.9);
        when(repositorio.buscarParaActualizar(anyList())).thenReturn(List.of(actual));

        conRepositorio.guardarResultados(List.of(resultado), true);

        assertEquals(-34.1, actual.getLatitud());
        assertEquals(-58.1, actual.getLongitud());
    }

    @Test
    @DisplayName("Con la integración apagada no escribe intentos")
    void apagadoNoGuardaIntentos() {
        DireccionJpaRepository repositorio = mock(DireccionJpaRepository.class);
        GestorGeoref apagado = gestorConRepositorio(repositorio, false);

        apagado.guardarResultados(List.of(direccionPersistida()), true);

        verifyNoInteractions(repositorio);
    }

    private GestorGeoref gestorConRepositorio(DireccionJpaRepository repositorio, boolean habilitado) {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        return new GestorGeoref(georef, repositorio, transactionManager, habilitado);
    }

    private static Direccion direccionPersistida() {
        Direccion direccion = direccion("Av. Medrano", 951, "Buenos Aires");
        direccion.setId(UUID.randomUUID());
        return direccion;
    }

    private static Direccion direccion(String calle, Integer altura, String nombreCiudad) {
        Pais pais = new Pais("Argentina");
        Provincia provincia = new Provincia("Buenos Aires", pais);
        Ciudad ciudad = new Ciudad(nombreCiudad, provincia);
        return new Direccion(calle, null, altura, 0, null, ciudad);
    }

    private static GeorefDTOs.Resultado resultado(double lat, double lon) {
        return new GeorefDTOs.Resultado(List.of(
                new GeorefDTOs.Direccion(new GeorefDTOs.Ubicacion(lat, lon))));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<List<GeorefDTOs.Consulta>> captorDeConsultas() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
    }
}
