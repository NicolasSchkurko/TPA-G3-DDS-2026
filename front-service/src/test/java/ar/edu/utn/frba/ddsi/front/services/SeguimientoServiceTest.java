package ar.edu.utn.frba.ddsi.front.services;

import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.SeguimientoDTO;
import ar.edu.utn.frba.ddsi.front.dto.logistica.LogisticaDTOs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SeguimientoServiceTest {

    @Mock
    private LogisticasClient logisticasClient;

    @Test
    void elCamionSeUbicaEnLaPrimeraParadaPendiente() {
        UUID idEntidad = UUID.randomUUID();
        UUID idDonacion = UUID.randomUUID();
        LogisticaDTOs.Ruta ruta = ruta(idEntidad, idDonacion);
        when(logisticasClient.obtenerRutasActivas()).thenReturn(List.of(ruta));
        when(logisticasClient.obtenerBienes()).thenReturn(new LogisticaDTOs.Bienes());

        SeguimientoDTO dto = new SeguimientoService(logisticasClient).construir(idEntidad, List.of());

        assertThat(dto.getCamiones()).hasSize(1);
        SeguimientoDTO.CamionEnRuta camion = dto.getCamiones().get(0);
        assertThat(camion.getParadaActual()).isEqualTo(2);
        assertThat(camion.getLat()).isEqualTo(-34.6375);
        assertThat(camion.getLng()).isEqualTo(-58.3816);
        assertThat(camion.getDestino()).isEqualTo("Av. La Plata 1500");
        assertThat(camion.getTrazado()).hasSize(3);
        assertThat(dto.getDepositoLat()).isEqualTo(-34.5986317);
        assertThat(dto.getDepositoEtiqueta()).isEqualTo("Depósito");
    }

    @Test
    void elDonanteVeElCamionDeSuRutaActiva() {
        UUID idEntidad = UUID.randomUUID();
        UUID idDonacion = UUID.randomUUID();
        LogisticaDTOs.Ruta ruta = ruta(idEntidad, idDonacion);
        when(logisticasClient.obtenerRutasActivas()).thenReturn(List.of(ruta));

        DonacionDTO donacion = new DonacionDTO();
        donacion.setId(idDonacion);
        donacion.setEstado("EN_TRASLADO");

        SeguimientoDTO dto = new SeguimientoService(logisticasClient)
                .construirDonante(List.of(donacion));

        assertThat(dto.getCamiones()).hasSize(1);
        assertThat(dto.getCamiones().get(0).getDonaciones()).containsExactly(idDonacion);
        assertThat(dto.getCamiones().get(0).getDestino()).isEqualTo("Av. La Plata 1500");
    }

    @Test
    void sinRutasConEntregasActivasNoInventaCamionesNiDeposito() {
        UUID idEntidad = UUID.randomUUID();
        LogisticaDTOs.Bien bien = new LogisticaDTOs.Bien();
        LogisticaDTOs.DireccionLogistica destino = new LogisticaDTOs.DireccionLogistica();
        destino.setIdEntidad(idEntidad);
        destino.setCiudad("CABA");
        destino.setLatitud(-34.6375);
        destino.setLongitud(-58.3816);
        bien.setEntidadDestino(destino);
        bien.setEstado("EN_CAMINO");
        bien.setCantidad(10);

        LogisticaDTOs.Bienes bienes = new LogisticaDTOs.Bienes();
        bienes.setIdsDonaciones(List.of(UUID.randomUUID()));
        bienes.setBienes(List.of(bien));

        when(logisticasClient.obtenerRutasActivas()).thenReturn(List.of());
        when(logisticasClient.obtenerBienes()).thenReturn(bienes);

        SeguimientoDTO dto = new SeguimientoService(logisticasClient).construir(idEntidad, List.of());

        assertThat(dto.getCamiones()).isEmpty();
        assertThat(dto.getEntregas()).hasSize(1);
        assertThat(dto.getDepositoLat()).isNull();
        assertThat(dto.getDepositoLng()).isNull();
        assertThat(dto.getDepositoEtiqueta()).isNull();
    }

    @Test
    void rutaRealNoTieneEtaYConservaDepositoReal() {
        UUID idEntidad = UUID.randomUUID();
        UUID idDonacion = UUID.randomUUID();
        LogisticaDTOs.Ruta ruta = ruta(idEntidad, idDonacion);
        when(logisticasClient.obtenerRutasActivas()).thenReturn(List.of(ruta));
        when(logisticasClient.obtenerBienes()).thenReturn(new LogisticaDTOs.Bienes());

        SeguimientoDTO dto = new SeguimientoService(logisticasClient).construir(idEntidad, List.of());

        assertThat(dto.getCamiones()).hasSize(1);
        assertThat(dto.getCamiones().get(0).getEtaMinutos()).isNull();
        assertThat(dto.getDepositoLat()).isEqualTo(-34.5986317);
        assertThat(dto.getDepositoEtiqueta()).isEqualTo("Depósito");
    }

    @Test
    void rutaSinDepositoNoInventaDeposito() {
        UUID idEntidad = UUID.randomUUID();
        UUID idDonacion = UUID.randomUUID();
        LogisticaDTOs.Ruta ruta = rutaSinDeposito(idEntidad, idDonacion);
        when(logisticasClient.obtenerRutasActivas()).thenReturn(List.of(ruta));
        when(logisticasClient.obtenerBienes()).thenReturn(new LogisticaDTOs.Bienes());

        SeguimientoDTO dto = new SeguimientoService(logisticasClient).construir(idEntidad, List.of());

        assertThat(dto.getCamiones()).hasSize(1);
        assertThat(dto.getDepositoLat()).isNull();
        assertThat(dto.getDepositoLng()).isNull();
        assertThat(dto.getDepositoEtiqueta()).isNull();
    }

    @Test
    void ultimaActualizacionUsaFechaRealDeCambioDeEstado() {
        UUID idEntidad = UUID.randomUUID();
        UUID idDonacion = UUID.randomUUID();
        LogisticaDTOs.Ruta ruta = rutaConFecha(idEntidad, idDonacion);
        when(logisticasClient.obtenerRutasActivas()).thenReturn(List.of(ruta));
        when(logisticasClient.obtenerBienes()).thenReturn(new LogisticaDTOs.Bienes());

        SeguimientoDTO dto = new SeguimientoService(logisticasClient).construir(idEntidad, List.of());

        assertThat(dto.getCamiones().get(0).getUltimaActualizacion()).isEqualTo("2026-06-12T10:30");
    }

    private static LogisticaDTOs.Ruta ruta(UUID idEntidad, UUID idDonacion) {
        LogisticaDTOs.Deposito deposito = new LogisticaDTOs.Deposito();
        deposito.setLatitud(-34.5986317);
        deposito.setLongitud(-58.4226973);
        deposito.setEtiqueta("Depósito");

        LogisticaDTOs.Ruta ruta = new LogisticaDTOs.Ruta();
        ruta.setIdRuta(UUID.randomUUID());
        ruta.setEstado("EN_CURSO");
        ruta.setDeposito(deposito);
        LogisticaDTOs.Camion camion = new LogisticaDTOs.Camion();
        camion.setPatente("AC482KP");
        ruta.setCamionAsignado(camion);
        ruta.setParadas(List.of(
                parada(idEntidad, "Av. Perito Moreno", 2400, -34.6538, -58.4478,
                        UUID.randomUUID(), "ENTREGADA"),
                parada(idEntidad, "Av. La Plata", 1500, -34.6375, -58.3816,
                        idDonacion, "EN_CAMINO")));
        return ruta;
    }

    private static LogisticaDTOs.Parada parada(UUID idEntidad, String calle, int altura,
                                               double lat, double lng, UUID idDonacion, String estado) {
        LogisticaDTOs.DireccionLogistica direccion = new LogisticaDTOs.DireccionLogistica();
        direccion.setIdEntidad(idEntidad);
        direccion.setCalleUno(calle);
        direccion.setAltura(altura);
        direccion.setCiudad("CABA");
        direccion.setLatitud(lat);
        direccion.setLongitud(lng);

        LogisticaDTOs.Bien bien = new LogisticaDTOs.Bien();
        bien.setIdDonacion(idDonacion);
        bien.setEstado(estado);
        bien.setCantidad(10);

        LogisticaDTOs.Bienes items = new LogisticaDTOs.Bienes();
        items.setIdsDonaciones(List.of(idDonacion));
        items.setBienes(List.of(bien));

        LogisticaDTOs.Parada parada = new LogisticaDTOs.Parada();
        parada.setDireccion(direccion);
        parada.setItems(items);
        return parada;
    }

    private static LogisticaDTOs.Ruta rutaSinDeposito(UUID idEntidad, UUID idDonacion) {
        LogisticaDTOs.Ruta ruta = ruta(idEntidad, idDonacion);
        ruta.setDeposito(null);
        return ruta;
    }

    private static LogisticaDTOs.Ruta rutaConFecha(UUID idEntidad, UUID idDonacion) {
        LogisticaDTOs.Ruta ruta = ruta(idEntidad, idDonacion);
        // marcar fecha de cambio de estado en el bien de la parada pendiente
        LogisticaDTOs.Parada parada = ruta.getParadas().get(1);
        parada.getItems().getBienes().get(0).setFechaCambioEstado(java.time.LocalDateTime.parse("2026-06-12T10:30"));
        return ruta;
    }
}
