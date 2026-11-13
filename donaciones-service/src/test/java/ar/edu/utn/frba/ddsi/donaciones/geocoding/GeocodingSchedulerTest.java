package ar.edu.utn.frba.ddsi.donaciones.geocoding;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorGeoref;
import ar.edu.utn.frba.ddsi.donaciones.models.scheduler.GeocodingScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Punto 75: el cron de backfill no debe correr indefinidamente ni marcar intentos cuando la
 * caída es de Georef y no de los datos. Punto 76: el scheduler solo habla con el gestor.
 */
class GeocodingSchedulerTest {

    private GestorGeoref gestor;

    @BeforeEach
    void setUp() {
        gestor = mock(GestorGeoref.class);
        when(gestor.isEnabled()).thenReturn(true);
    }

    @Test
    @DisplayName("Procesa un lote, guarda los cambios y corta cuando no quedan pendientes")
    void procesaYGuarda() {
        Direccion una = direccion("Av. Medrano");
        Direccion otra = direccion("Av. Corrientes");
        when(gestor.buscarPendientes(any(), anyInt(), anyInt()))
                .thenReturn(List.of(una, otra))
                .thenReturn(List.of());
        when(gestor.completarCoordenadas(anyList())).thenReturn(true);

        scheduler(100).geocodificarPendientes();

        verify(gestor).completarCoordenadas(List.of(una, otra));
        verify(gestor).guardarResultados(List.of(una, otra), true);
        verify(gestor, times(2)).buscarPendientes(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Si Georef cae, guarda lo resuelto y corta: no sigue con el resto de los pendientes")
    void cortaSiGeorefFalla() {
        Direccion una = direccion("Av. Medrano");
        when(gestor.buscarPendientes(any(), anyInt(), anyInt()))
                .thenReturn(List.of(una));
        when(gestor.completarCoordenadas(anyList())).thenReturn(false);

        scheduler(100).geocodificarPendientes();

        verify(gestor).guardarResultados(List.of(una), false);
        verify(gestor, times(1)).buscarPendientes(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Respeta el máximo de direcciones por corrida")
    void respetaMaximoPorCorrida() {
        Direccion una = direccion("Av. Medrano");
        when(gestor.buscarPendientes(any(), anyInt(), anyInt()))
                .thenReturn(List.of(una));
        when(gestor.completarCoordenadas(anyList())).thenReturn(true);

        scheduler(2).geocodificarPendientes();

        verify(gestor, times(2)).completarCoordenadas(List.of(una));
        verify(gestor, times(2)).guardarResultados(List.of(una), true);
    }

    @Test
    @DisplayName("Con el cron apagado no consulta al gestor")
    void apagadoNoConsulta() {
        GeocodingScheduler apagado = new GeocodingScheduler(
                gestor, false, 1000, 7, 3);

        apagado.geocodificarPendientes();

        verify(gestor, never()).buscarPendientes(any(), anyInt(), anyInt());
        verify(gestor, never()).completarCoordenadas(anyList());
    }

    @Test
    @DisplayName("Con el geocoding apagado tampoco consulta al gestor")
    void geocodingApagadoNoConsulta() {
        when(gestor.isEnabled()).thenReturn(false);

        scheduler(100).geocodificarPendientes();

        verify(gestor, never()).buscarPendientes(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Pasa al gestor el backoff y el máximo de intentos configurados")
    void pasaConfiguracionDeReintentos() {
        when(gestor.buscarPendientes(any(), anyInt(), anyInt())).thenReturn(List.of());

        new GeocodingScheduler(gestor, true, 1000, 7, 3)
                .geocodificarPendientes();

        verify(gestor).buscarPendientes(any(), eq(3), eq(1000));
    }

    private GeocodingScheduler scheduler(int maxPorCorrida) {
        return new GeocodingScheduler(gestor, true, maxPorCorrida, 7, 3);
    }

    private static Direccion direccion(String calle) {
        Pais pais = new Pais("Argentina");
        Provincia provincia = new Provincia("Buenos Aires", pais);
        Ciudad ciudad = new Ciudad("Buenos Aires", provincia);
        return new Direccion(calle, null, 100, 0, null, ciudad);
    }
}
