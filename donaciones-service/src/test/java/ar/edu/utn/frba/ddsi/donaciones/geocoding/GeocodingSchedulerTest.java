package ar.edu.utn.frba.ddsi.donaciones.geocoding;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorGeoref;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.DireccionJpaRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.sheduler.GeocodingScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Punto 75: el cron de backfill no debe correr indefinidamente ni marcar intentos cuando la
 * caída es de Georef y no de los datos.
 */
class GeocodingSchedulerTest {

    private DireccionJpaRepository repositorio;
    private GestorGeoref gestor;

    @BeforeEach
    void setUp() {
        repositorio = mock(DireccionJpaRepository.class);
        gestor = mock(GestorGeoref.class);
        when(gestor.isEnabled()).thenReturn(true);
    }

    @Test
    @DisplayName("Procesa un lote, guarda los cambios y corta cuando no quedan pendientes")
    void procesaYGuarda() {
        Direccion una = direccion("Av. Medrano");
        Direccion otra = direccion("Av. Corrientes");
        when(repositorio.buscarPendientesGeocoding(any(), anyInt(), any(Pageable.class)))
                .thenReturn(List.of(una, otra))
                .thenReturn(List.of());
        when(gestor.completarCoordenadas(anyList())).thenReturn(true);

        scheduler(100).geocodificarPendientes();

        verify(gestor).completarCoordenadas(List.of(una, otra));
        verify(gestor).guardarResultados(List.of(una, otra), true);
        verify(repositorio, times(2)).buscarPendientesGeocoding(any(), anyInt(), any(Pageable.class));
    }

    @Test
    @DisplayName("Si Georef cae, guarda lo resuelto y corta: no sigue con el resto de los pendientes")
    void cortaSiGeorefFalla() {
        Direccion una = direccion("Av. Medrano");
        when(repositorio.buscarPendientesGeocoding(any(), anyInt(), any(Pageable.class)))
                .thenReturn(List.of(una));
        when(gestor.completarCoordenadas(anyList())).thenReturn(false);

        scheduler(100).geocodificarPendientes();

        verify(gestor).guardarResultados(List.of(una), false);
        verify(repositorio, times(1)).buscarPendientesGeocoding(any(), anyInt(), any(Pageable.class));
    }

    @Test
    @DisplayName("Respeta el máximo de direcciones por corrida")
    void respetaMaximoPorCorrida() {
        Direccion una = direccion("Av. Medrano");
        when(repositorio.buscarPendientesGeocoding(any(), anyInt(), any(Pageable.class)))
                .thenReturn(List.of(una));
        when(gestor.completarCoordenadas(anyList())).thenReturn(true);

        scheduler(2).geocodificarPendientes();

        verify(gestor, times(2)).completarCoordenadas(List.of(una));
        verify(gestor, times(2)).guardarResultados(List.of(una), true);
    }

    @Test
    @DisplayName("Con el cron apagado no consulta la base")
    void apagadoNoConsulta() {
        GeocodingScheduler apagado = new GeocodingScheduler(
                repositorio, gestor, false, 1000, 7, 3);

        apagado.geocodificarPendientes();

        verifyNoInteractions(repositorio);
        verify(gestor, never()).completarCoordenadas(anyList());
    }

    @Test
    @DisplayName("Con el geocoding apagado tampoco consulta la base")
    void geocodingApagadoNoConsulta() {
        when(gestor.isEnabled()).thenReturn(false);

        scheduler(100).geocodificarPendientes();

        verifyNoInteractions(repositorio);
    }

    @Test
    @DisplayName("Pasa al repositorio el backoff y el máximo de intentos configurados")
    void pasaConfiguracionDeReintentos() {
        when(repositorio.buscarPendientesGeocoding(any(), anyInt(), any(Pageable.class)))
                .thenReturn(List.of());

        new GeocodingScheduler(repositorio, gestor, true, 1000, 7, 3)
                .geocodificarPendientes();

        verify(repositorio).buscarPendientesGeocoding(
                any(), eq(3), eq(org.springframework.data.domain.PageRequest.of(0, 1000)));
    }

    private GeocodingScheduler scheduler(int maxPorCorrida) {
        return new GeocodingScheduler(repositorio, gestor, true, maxPorCorrida, 7, 3);
    }

    private static Direccion direccion(String calle) {
        Pais pais = new Pais("Argentina");
        Provincia provincia = new Provincia("Buenos Aires", pais);
        Ciudad ciudad = new Ciudad("Buenos Aires", provincia);
        return new Direccion(calle, null, 100, 0, null, ciudad);
    }
}
